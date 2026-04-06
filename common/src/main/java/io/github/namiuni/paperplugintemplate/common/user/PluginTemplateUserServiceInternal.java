/*
 * PaperPluginTemplate
 *
 * Copyright (c) 2026. Namiu (うにたろう)
 *                     Contributors []
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package io.github.namiuni.paperplugintemplate.common.user;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import io.github.namiuni.paperplugintemplate.api.user.PluginTemplateUser;
import io.github.namiuni.paperplugintemplate.api.user.PluginTemplateUserService;
import io.github.namiuni.paperplugintemplate.common.configuration.PrimaryConfiguration;
import io.github.namiuni.paperplugintemplate.common.user.storage.UserProfile;
import io.github.namiuni.paperplugintemplate.common.user.storage.UserRepository;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.identity.Identified;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.jspecify.annotations.NullMarked;

/// Application service for managing player [UserProfile] data.
///
/// ## Dual-cache architecture
///
/// Two Caffeine caches work in tandem to eliminate repository round-trips
/// during normal gameplay while bounding memory use.
///
/// ### Pre-load cache (`preloadCache`)
///
/// A short-lived `Cache<UUID, UserProfile>` populated before a `Player`
/// object exists. Entries expire 30 seconds after write regardless of
/// access. Its sole purpose is to bridge the gap between the async
/// connection event and the first [#loadUser] call on join. Once consumed,
/// the preloaded profile is promoted into `userCache` and the preload entry
/// expires naturally — no explicit removal is needed.
///
/// ### User cache (`userCache`)
///
/// A `Cache<UUID, PluginTemplateUser>` governed by [OnlineAwareExpiry]:
///
/// - **Online players** (`isOnline() == true`): pinned indefinitely. The
///   only removal path is explicit eviction via [#discardUser] on
///   disconnect.
/// - **Offline players** (`isOnline() == false`): evicted after the
///   configured `expireAfterOffline` duration following the last cache
///   interaction.
///
/// ## Thread safety
///
/// Both Caffeine caches are thread-safe. All methods on this service may be
/// called from any thread, including Paper's async event threads and virtual
/// threads. No `synchronized` blocks are used; this class is therefore free
/// from virtual-thread carrier-thread pinning (JEP 491).
///
/// @see UserRepository
@Singleton
@NullMarked
public final class PluginTemplateUserServiceInternal implements PluginTemplateUserService {

    private final UserRepository repository;
    private final UserFactory userFactory;
    private final ComponentLogger logger;

    private final Cache<UUID, UserProfile> preloadCache;
    private final Cache<UUID, PluginTemplateUser> userCache;

    /// Constructs a new service, initializing both in-memory caches.
    ///
    /// @param repository    the storage backend used for cold-cache misses and
    ///                      persistence operations
    /// @param userFactory   the platform-specific factory used to construct
    ///                      user adapter instances
    /// @param primaryConfig the configuration supplier providing cache settings
    /// @param logger        the component-aware logger
    @Inject
    private PluginTemplateUserServiceInternal(
            final UserRepository repository,
            final UserFactory userFactory,
            final Supplier<PrimaryConfiguration> primaryConfig,
            final ComponentLogger logger
    ) {
        this.repository = repository;
        this.userFactory = userFactory;
        this.logger = logger;

        final PrimaryConfiguration.Storage.Cache cacheSettings = primaryConfig.get().storage().userCache();
        this.preloadCache = Caffeine.newBuilder()
                .expireAfterWrite(30L, TimeUnit.SECONDS)
                .build();
        this.userCache = Caffeine.newBuilder()
                .maximumSize(cacheSettings.maximumSize())
                .expireAfter(new OnlineAwareExpiry(cacheSettings))
                .build();

        this.logger.debug(
                "User cache initialized: maximumSize={}, expireAfterOffline={}ns.",
                cacheSettings.maximumSize(),
                cacheSettings.expireAfterOffline()
        );
    }

    /// Pre-loads a player's profile into the preload cache so that the
    /// subsequent [#loadUser] call can resolve without a repository
    /// round-trip.
    ///
    /// If the profile is already present in `userCache` (e.g. the player
    /// reconnects quickly after a disconnect), the cached entry is returned
    /// immediately without touching the repository.
    ///
    /// Called during `AsyncPlayerConnectionConfigureEvent`, where no live
    /// `Player` object exists yet.
    ///
    /// @param uuid the connecting player's UUID
    /// @return a future resolving to the profile wrapped in [Optional], or
    ///         [Optional#empty()] for first-time players; may complete
    ///         exceptionally if repository I/O fails
    public CompletableFuture<Optional<UserProfile>> loadUserProfile(final UUID uuid) {
        final PluginTemplateUser cached = this.userCache.getIfPresent(uuid);
        if (cached instanceof final PluginTemplateUserInternal platformUser) {
            this.logger.debug("[preload] userCache hit for {} — skipping repository.", uuid);
            return CompletableFuture.completedFuture(Optional.of(platformUser.profile()));
        }

        final UserProfile preloaded = this.preloadCache.getIfPresent(uuid);
        if (preloaded != null) {
            this.logger.debug("[preload] preloadCache hit for {}.", uuid);
            return CompletableFuture.completedFuture(Optional.of(preloaded));
        }

        this.logger.debug("[preload] Cold miss for {} — querying repository.", uuid);
        return this.repository.findById(uuid)
                .thenApply(existing -> {
                    if (existing.isPresent()) {
                        this.preloadCache.put(uuid, existing.get());
                        this.logger.debug("[preload] Profile stored in preloadCache for {}.", uuid);
                    } else {
                        this.logger.debug("[preload] No existing profile for {} (first join).", uuid);
                    }
                    return existing;
                });
    }

    /// Persists the current state of `user` to storage without modifying
    /// any system-managed fields.
    ///
    /// @param user the user whose current profile state should be saved;
    ///             must not be `null`
    /// @return a future that completes when the repository write finishes,
    ///         or completes immediately with `null` if `user` does not
    ///         implement [PluginTemplateUserInternal]
    public CompletableFuture<Void> upsertUser(final PluginTemplateUser user) {
        if (user instanceof final PluginTemplateUserInternal platformUser) {
            final UserProfile current = platformUser.profile();
            this.logger.debug("[upsert] Persisting profile for {} ({}).", current.uuid(), current.name());
            this.userCache.put(current.uuid(), platformUser);
            return this.repository.upsert(current);
        }
        return CompletableFuture.completedFuture(null);
    }

    /// Stamps [UserProfile#lastSeen()] with the current instant, persists
    /// the updated profile, and then **evicts** the cache entry.
    ///
    /// This method is exclusively for the **player disconnect** path.
    ///
    /// @param user the online player whose session is ending; must not be `null`
    /// @return a future that completes when the repository write finishes
    /// @see #checkpointUser
    public CompletableFuture<Void> persistOnlinePlayer(final PluginTemplateUser user) {
        if (user instanceof final PluginTemplateUserInternal platformUser) {
            platformUser.updateProfile(profile -> profile.withLastSeen(Instant.now()));
            final UserProfile updated = platformUser.profile();
            this.logger.debug("[persist] Disconnecting {} ({}) — stamping lastSeen and persisting.", updated.uuid(), updated.name());
            return this.repository.upsert(updated)
                    .whenComplete((_, ex) -> {
                        if (ex != null) {
                            this.logger.debug("[persist] Repository write failed for {} — evicting anyway.", updated.uuid());
                        } else {
                            this.logger.debug("[persist] Profile persisted for {}.", updated.uuid());
                        }
                        this.discardUser(updated.uuid());
                    });
        }
        return CompletableFuture.completedFuture(null);
    }

    /// Stamps [UserProfile#lastSeen()] with the current instant and persists
    /// the updated profile, **without** evicting the cache entry.
    ///
    /// This method is exclusively for the **periodic checkpoint** path.
    ///
    /// @param user the online player to checkpoint; must not be `null`
    /// @return a future that completes when the repository write finishes
    /// @see #persistOnlinePlayer
    public CompletableFuture<Void> checkpointUser(final PluginTemplateUser user) {
        if (user instanceof final PluginTemplateUserInternal platformUser) {
            platformUser.updateProfile(profile -> profile.withLastSeen(Instant.now()));
            final UserProfile updated = platformUser.profile();
            this.logger.debug("[checkpoint] Checkpointing {} ({}).", updated.uuid(), updated.name());
            this.userCache.put(updated.uuid(), platformUser);
            return this.repository.upsert(updated)
                    .whenComplete((_, ex) -> {
                        if (ex != null) {
                            this.logger.debug("[checkpoint] Repository write failed for {}.", updated.uuid());
                        }
                    });
        }
        return CompletableFuture.completedFuture(null);
    }

    /// Evicts the entry for `uuid` from the user cache.
    ///
    /// This method is idempotent: calling it multiple times for the same
    /// `uuid` is safe and has no additional effect after the first call.
    ///
    /// @param uuid the player UUID to evict
    public void discardUser(final UUID uuid) {
        this.logger.debug("[discard] Evicting cache entry for {}.", uuid);
        this.userCache.invalidate(uuid);
    }

    /// {@inheritDoc}
    @Override
    public Optional<PluginTemplateUser> getUser(final UUID uuid) {
        return Optional.ofNullable(this.userCache.getIfPresent(uuid));
    }

    /// {@inheritDoc}
    @Override
    public <P extends Audience & Identified> CompletableFuture<PluginTemplateUser> loadUser(final P player) {
        final UUID uuid = player.get(Identity.UUID).orElseThrow();

        // 1. User cache — non-blocking, always preferred.
        final PluginTemplateUser cached = this.userCache.getIfPresent(uuid);
        if (cached != null) {
            this.logger.debug("[loadUser] Tier-1 (userCache) hit for {}.", uuid);
            return CompletableFuture.completedFuture(cached);
        }

        final String currentName = player.get(Identity.NAME).orElseThrow();

        // 2. Preload cache — profile was fetched during onConnection.
        final UserProfile preloaded = this.preloadCache.getIfPresent(uuid);
        if (preloaded != null) {
            this.logger.debug("[loadUser] Tier-2 (preloadCache) hit for {} ({}) — promoting to userCache.", uuid, currentName);
            final PluginTemplateUser platformUser = this.userFactory.create(player, preloaded);
            this.userCache.put(uuid, platformUser);
            return CompletableFuture.completedFuture(platformUser);
        }

        // 3. Repository — async I/O; new players receive a default profile.
        this.logger.debug("[loadUser] Tier-3 (repository) miss for {} ({}) — querying storage.", uuid, currentName);
        return this.repository.findById(uuid)
                .thenApply(existing -> {
                    if (existing.isEmpty()) {
                        this.logger.debug("[loadUser] No profile found for {} — creating default.", uuid);
                    }
                    return existing
                            .map(profile -> new UserProfile(profile.uuid(), currentName, profile.lastSeen()))
                            .orElse(new UserProfile(uuid, currentName, Instant.now()));
                })
                .thenApply(profile -> {
                    final PluginTemplateUser platformUser = this.userFactory.create(player, profile);
                    this.userCache.put(uuid, platformUser);
                    this.logger.debug("[loadUser] Profile cached for {} ({}).", uuid, currentName);
                    return platformUser;
                });
    }

    /// {@inheritDoc}
    @Override
    public CompletableFuture<Void> deleteUser(final UUID uuid) {
        this.logger.debug("[delete] Removing all data for {}.", uuid);
        this.userCache.invalidate(uuid);
        this.preloadCache.invalidate(uuid);
        return this.repository.delete(uuid);
    }

    // -------------------------------------------------------------------------
    // Cache expiry policy
    // -------------------------------------------------------------------------

    /// Caffeine [Expiry] policy that pins online players indefinitely in
    /// cache and applies access-based expiry to offline players.
    ///
    /// @implNote TTL values are in nanoseconds as required by the Caffeine API.
    ///           `Long.MAX_VALUE` nanoseconds is effectively infinite (~292 years).
    private static final class OnlineAwareExpiry implements Expiry<UUID, PluginTemplateUser> {

        private static final long NEVER_EXPIRE_NANOS = Long.MAX_VALUE;

        private final long offlineExpireNanos;

        OnlineAwareExpiry(final PrimaryConfiguration.Storage.Cache cacheSettings) {
            this.offlineExpireNanos = cacheSettings.expireAfterOffline();
        }

        @Override
        public long expireAfterCreate(final UUID key, final PluginTemplateUser user, final long currentTime) {
            return this.ttl(user);
        }

        @Override
        public long expireAfterUpdate(
                final UUID key, final PluginTemplateUser user,
                final long currentTime, final long currentDuration
        ) {
            return this.ttl(user);
        }

        @Override
        public long expireAfterRead(
                final UUID key, final PluginTemplateUser user,
                final long currentTime, final long currentDuration
        ) {
            return this.ttl(user);
        }

        private long ttl(final PluginTemplateUser user) {
            return user.isOnline() ? NEVER_EXPIRE_NANOS : this.offlineExpireNanos;
        }
    }
}

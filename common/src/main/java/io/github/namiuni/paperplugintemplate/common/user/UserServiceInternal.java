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
import io.github.namiuni.paperplugintemplate.common.component.ComponentStore;
import io.github.namiuni.paperplugintemplate.common.infrastructure.configuration.configurations.PrimaryConfiguration;
import io.github.namiuni.paperplugintemplate.common.infrastructure.persistence.UserRecord;
import io.github.namiuni.paperplugintemplate.common.infrastructure.persistence.UserRepository;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.identity.Identified;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.jspecify.annotations.NullMarked;

/// Internal [PluginTemplateUserService] implementation that manages the full
/// lifecycle of plugin-tracked players.
///
/// ## Three-tier resolution
///
/// [#loadUser] resolves through three cache tiers in order, stopping at the
/// first hit:
///
/// 1. **User cache** — the primary in-memory cache holding fully initialized
///    [PluginTemplateUser] instances. Non-blocking; always preferred.
/// 2. **Preload cache** — populated during
///    [io.papermc.paper.event.connection.configuration.AsyncPlayerConnectionConfigureEvent],
///    before the `Player` object exists. Allows [#loadUser] to be synchronous
///    for returning players whose profile was already fetched.
/// 3. **Repository** — asynchronous I/O via [UserRepository]. Only reached
///    on a cold miss. New players receive a default profile.
///
/// ## Cache expiry
///
/// The user cache uses a custom [com.github.benmanes.caffeine.cache.Expiry]
/// policy: online players (where [PluginTemplateUser#isOnline()] returns
/// `true`) are pinned indefinitely; offline players expire
/// `expireAfterOffline` nanoseconds after their last cache access.
///
/// On eviction, all ECS components registered under the evicted UUID are
/// purged from the [ComponentStore] to prevent unbounded memory growth.
///
/// ## Thread safety
///
/// All public methods are safe to call from any thread. The Caffeine caches
/// are themselves thread-safe, and all repository interactions are dispatched
/// to a virtual-thread executor.
@Singleton
@NullMarked
public final class UserServiceInternal implements PluginTemplateUserService {

    private final UserRepository repository;
    private final UserFactory userFactory;
    private final ComponentLogger logger;

    private final Cache<UUID, UserRecord> preloadCache;   // UUID → UserRecord (pre-join snapshot)
    private final Cache<UUID, PluginTemplateUser> userCache;

    /// Constructs a new service, initializing both in-memory caches.
    ///
    /// @param repository      the storage backend used for cold-cache misses and
    ///                        persistence operations
    /// @param userFactory     the platform-specific factory used to construct user
    ///                        adapter instances
    /// @param componentStore  the shared ECS store; components are purged from this
    ///                        store when a user's cache entry is evicted
    /// @param primaryConfig   the configuration provider supplying cache settings
    /// @param logger          the component-aware logger
    @Inject
    private UserServiceInternal(
            final UserRepository repository,
            final UserFactory userFactory,
            final ComponentStore componentStore,
            final Provider<PrimaryConfiguration> primaryConfig,
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
                .removalListener((uuid, _, _) -> {
                    if (uuid != null) {
                        componentStore.removeAll(uuid);
                    }
                })
                .build();
    }

    /// Pre-loads a player's persistent record into the preload cache so that
    /// the subsequent [#loadUser] call can resolve without a repository
    /// round-trip.
    ///
    /// If the record is already present in the user cache (e.g. the player
    /// reconnects quickly after a disconnect), the cached entry is reused
    /// immediately without touching the repository.
    ///
    /// Called during `AsyncPlayerConnectionConfigureEvent`, before the live
    /// `Player` object exists.
    ///
    /// @param uuid       the connecting player's UUID
    /// @param disconnect a callback invoked when the repository lookup fails;
    ///                   implementations should disconnect the client gracefully
    /// @return a future that completes with `null` on success; if the repository
    ///         is unreachable the future still completes normally after invoking
    ///         `disconnect`
    public CompletableFuture<Void> loadUserRecord(final UUID uuid, final Runnable disconnect) {
        final PluginTemplateUser cachedUser = this.userCache.getIfPresent(uuid);
        if (cachedUser instanceof UserInternal) {
            this.logger.debug("[preload] userCache hit for {} — skipping repository.", uuid);
            return CompletableFuture.completedFuture(null);
        }

        final UserRecord preloaded = this.preloadCache.getIfPresent(uuid);
        if (preloaded != null) {
            this.logger.debug("[preload] preloadCache hit for {}.", uuid);
            return CompletableFuture.completedFuture(null);
        }

        this.logger.debug("[preload] Cold miss for {} — querying repository.", uuid);
        return this.repository.findById(uuid)
                .thenAccept(existing -> {
                    if (existing.isPresent()) {
                        this.preloadCache.put(uuid, existing.get());
                        this.logger.debug("[preload] Profile stored in preloadCache for {}.", uuid);
                    } else {
                        this.logger.debug("[preload] No existing profile for {} (first join).", uuid);
                    }
                })
                .whenComplete((_, ex) -> {
                    if (ex != null) {
                        this.logger.error("Failed to pre-load profile for UUID: {}; disconnecting.", uuid, ex);
                        disconnect.run();
                    }
                });
    }

    /// Persists the current state of the user identified by `uuid` to storage.
    ///
    /// A no-op if no user is currently cached for `uuid`.
    ///
    /// @param uuid the UUID of the user to persist
    /// @return a future that completes when the repository write finishes, or
    ///         completes immediately with `null` if no cached user exists
    public CompletableFuture<Void> saveUser(final UUID uuid) {
        final var user = this.userCache.getIfPresent(uuid);
        if (user != null) {
            final var userRecord = new UserRecord(
                    user.uuid(),
                    user.name(),
                    user.lastSeen()
            );
            return this.repository.upsert(userRecord)
                    .whenComplete((_, exception) -> {
                        if (exception != null) {
                            this.logger.error("Failed save user record on disconnect for UUID: {}", uuid, exception);
                        }
                    });
        }

        return CompletableFuture.completedFuture(null);
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
        final String currentName = player.get(Identity.NAME).orElseThrow();

        // 1. User cache — non-blocking, always preferred.
        final PluginTemplateUser cached = this.userCache.getIfPresent(uuid);
        if (cached != null) {
            this.logger.debug("[loadUser] Tier-1 (userCache) hit for {}.", uuid);
            return CompletableFuture.completedFuture(cached);
        }

        // 2. Preload cache — profile was fetched during onConnect.
        final UserRecord preloaded = this.preloadCache.getIfPresent(uuid);
        if (preloaded != null) {
            final PluginTemplateUser user = this.userFactory.create(player, preloaded);
            this.userCache.put(uuid, user);
            return CompletableFuture.completedFuture(user);
        }

        // 3. Repository — async I/O; new players receive a default profile.
        this.logger.debug("[loadUser] Tier-3 (repository) miss for {} ({}) — querying storage.", uuid, currentName);
        return this.repository.findById(uuid)
                .thenApply(existing -> existing.orElse(new UserRecord(uuid, currentName, Instant.now())))
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

    /// Caffeine [Expiry] policy that pins online players indefinitely in the
    /// user cache and applies access-based expiry to offline players.
    ///
    /// Online status is re-evaluated on every cache interaction by calling
    /// [PluginTemplateUser#isOnline()], keeping the TTL dynamic without
    /// external invalidation.
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

/*
 * PaperPluginTemplate
 *
 * Copyright (c) 2026. Namiu (うにたろう)
 *                     Contributors
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
import io.github.namiuni.paperplugintemplate.common.user.storage.UserProfile;
import io.github.namiuni.paperplugintemplate.common.user.storage.UserRepository;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.identity.Identified;
import net.kyori.adventure.identity.Identity;
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
/// - **Offline players** (`isOnline() == false`): evicted 15 minutes after
///   the last cache interaction, preventing unbounded memory growth for
///   offline lookups.
///
/// ## Persist vs. checkpoint distinction
///
/// Two separate write paths exist for profile persistence and must not be
/// confused:
///
/// - [#persistOnlinePlayer]: **disconnect only**. Stamps `lastSeen`, persists
///   to the repository, then **evicts** the cache entry. Calling this while
///   the player is still online removes them from the cache, causing
///   subsequent [#getUser] calls to return `Optional.empty()`.
/// - [#checkpointUser]: **periodic saves only** (e.g. world-save events).
///   Stamps `lastSeen` and persists to the repository, but **retains** the
///   cache entry so that all subsequent service calls remain cache hits for
///   the duration of the session.
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

    private final Cache<UUID, UserProfile> preloadCache;
    private final Cache<UUID, PluginTemplateUser> userCache;

    /// Constructs a new service, initializing both in-memory caches.
    ///
    /// @param repository  the storage backend used for cold-cache misses and
    ///                    persistence operations
    /// @param userFactory the platform-specific factory used to construct
    ///                    user adapter instances
    @Inject
    private PluginTemplateUserServiceInternal(
            final UserRepository repository,
            final UserFactory userFactory
    ) {
        this.repository = repository;
        this.userFactory = userFactory;

        this.preloadCache = Caffeine.newBuilder()
                .maximumSize(512L)
                .expireAfterWrite(30L, TimeUnit.SECONDS)
                .build();
        this.userCache = Caffeine.newBuilder()
                .maximumSize(512L)
                .expireAfter(new OnlineAwareExpiry())
                .build();
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
    /// @apiNote This method is intentionally internal. Callers outside this
    ///          service should use [#loadUser] instead.
    /// @implNote Completes on a virtual-thread executor for the repository
    ///           path; completes on the calling thread for cache-hit paths.
    public CompletableFuture<Optional<UserProfile>> loadUserProfile(final UUID uuid) {
        final PluginTemplateUser cached = this.userCache.getIfPresent(uuid);
        if (cached instanceof final PluginTemplateUserInternal platformUser) {
            return CompletableFuture.completedFuture(Optional.of(platformUser.profile()));
        }

        final UserProfile preloaded = this.preloadCache.getIfPresent(uuid);
        if (preloaded != null) {
            return CompletableFuture.completedFuture(Optional.of(preloaded));
        }

        return this.repository.findById(uuid)
                .thenApply(existing -> {
                    existing.ifPresent(profile -> this.preloadCache.put(uuid, profile));
                    return existing;
                });
    }

    /// Persists the current state of `user` to storage without modifying
    /// any system-managed fields.
    ///
    /// The `instanceof` pattern match against [PluginTemplateUserInternal]
    /// replaces the former cast to {@code PlatformUser}: any value returned
    /// by [UserFactory#create] satisfies this check, and mock objects used in
    /// tests that implement only [PluginTemplateUser] safely reach the
    /// no-op branch.
    ///
    /// @param user the user whose current profile state should be saved;
    ///             must not be `null`
    /// @return a future that completes when the repository write finishes,
    ///         or completes immediately with `null` if `user` does not
    ///         implement [PluginTemplateUserInternal]
    public CompletableFuture<Void> upsertUser(final PluginTemplateUser user) {
        if (user instanceof final PluginTemplateUserInternal platformUser) {
            final UserProfile current = platformUser.profile();
            // Re-put to trigger expireAfterUpdate so the expiry policy
            // re-evaluates isOnline() and recalculates the TTL.
            this.userCache.put(current.uuid(), platformUser);
            return this.repository.upsert(current);
        }
        return CompletableFuture.completedFuture(null);
    }

    /// Stamps [UserProfile#lastSeen()] with the current instant, persists
    /// the updated profile, and then **evicts** the cache entry.
    ///
    /// This method is exclusively for the **player disconnect** path.
    /// Cache eviction is guaranteed via
    /// [java.util.concurrent.CompletableFuture#whenComplete], so the entry
    /// is removed regardless of whether the repository write succeeds or
    /// fails.
    ///
    /// **Do not call this method for periodic saves such as world-save
    /// checkpoints.** Evicting an online player from the cache causes all
    /// subsequent [#getUser] calls to return `Optional.empty()` until
    /// [#loadUser] is called again. Use [#checkpointUser] for periodic
    /// saves instead.
    ///
    /// @param user the online player whose session is ending; must not be
    ///             `null`
    /// @return a future that completes when the repository write finishes;
    ///         the cache entry is evicted unconditionally after completion,
    ///         or completes immediately with `null` if `user` does not
    ///         implement [PluginTemplateUserInternal]
    /// @see #checkpointUser
    public CompletableFuture<Void> persistOnlinePlayer(final PluginTemplateUser user) {
        if (user instanceof final PluginTemplateUserInternal platformUser) {
            platformUser.updateProfile(profile -> profile.withLastSeen(Instant.now()));
            final UserProfile updated = platformUser.profile();
            return this.repository.upsert(updated)
                    .whenComplete((_, _) -> this.discardUser(updated.uuid()));
        }
        return CompletableFuture.completedFuture(null);
    }

    /// Stamps [UserProfile#lastSeen()] with the current instant and persists
    /// the updated profile, **without** evicting the cache entry.
    ///
    /// This method is exclusively for the **periodic checkpoint** path (e.g.
    /// world-save events). Unlike [#persistOnlinePlayer], it retains the
    /// cache entry so that the player remains a cache hit for the duration of
    /// their session. The re-put also triggers [OnlineAwareExpiry] to
    /// re-evaluate `isOnline()` and recalculate the TTL.
    ///
    /// **Do not use this method for disconnect handling.** The cache entry
    /// will not be evicted, and the `lastSeen` timestamp recorded by this
    /// method will be overwritten by the disconnect path.
    ///
    /// @param user the online player to checkpoint; must not be `null`
    /// @return a future that completes when the repository write finishes;
    ///         may complete exceptionally if the repository write fails.
    ///         Completes immediately with `null` if `user` does not
    ///         implement [PluginTemplateUserInternal]
    /// @see #persistOnlinePlayer
    public CompletableFuture<Void> checkpointUser(final PluginTemplateUser user) {
        if (user instanceof final PluginTemplateUserInternal platformUser) {
            platformUser.updateProfile(profile -> profile.withLastSeen(Instant.now()));
            final UserProfile updated = platformUser.profile();
            // Re-put to trigger expireAfterUpdate; does NOT evict the entry.
            this.userCache.put(updated.uuid(), platformUser);
            return this.repository.upsert(updated);
        }
        return CompletableFuture.completedFuture(null);
    }

    /// Evicts the entry for `uuid` from the user cache.
    ///
    /// Calling this method while the player is still online removes them
    /// from the pinned-forever expiry bucket; a subsequent cache miss will
    /// rebuild their entry from the repository.
    ///
    /// This method is idempotent: calling it multiple times for the same
    /// `uuid` is safe and has no additional effect after the first call.
    ///
    /// @param uuid the player UUID to evict
    public void discardUser(final UUID uuid) {
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
            return CompletableFuture.completedFuture(cached);
        }

        final String currentName = player.get(Identity.NAME).orElseThrow();

        // 2. Preload cache — profile was fetched during onConnection; avoid a
        //    second round-trip to the repository.
        final UserProfile preloaded = this.preloadCache.getIfPresent(uuid);
        if (preloaded != null) {
            // Reflect any username change that occurred between the last session
            // and this connection before promoting to the main cache.
            final PluginTemplateUser platformUser = this.userFactory.create(player, preloaded);
            this.userCache.put(uuid, platformUser);
            return CompletableFuture.completedFuture(platformUser);
        }

        // 3. Repository — async I/O; new players receive a default profile.
        return this.repository.findById(uuid)
                .thenApply(existing -> existing
                        .map(profile -> new UserProfile(profile.uuid(), currentName, profile.lastSeen()))
                        .orElse(new UserProfile(uuid, currentName, Instant.now())))
                .thenApply(profile -> {
                    final PluginTemplateUser platformUser = this.userFactory.create(player, profile);
                    this.userCache.put(uuid, platformUser);
                    return platformUser;
                });
    }

    /// {@inheritDoc}
    @Override
    public CompletableFuture<Void> deleteUser(final UUID uuid) {
        this.userCache.invalidate(uuid);
        this.preloadCache.invalidate(uuid);
        return this.repository.delete(uuid);
    }

    // -------------------------------------------------------------------------
    // Cache expiry policy
    // -------------------------------------------------------------------------

    /// Caffeine [Expiry] policy that pins online players indefinitely in
    /// cache and applies 15-minute access-based expiry to offline players.
    ///
    /// All three lifecycle callbacks — [#expireAfterCreate],
    /// [#expireAfterUpdate], and [#expireAfterRead] — delegate to the same
    /// [#ttl] helper. If [PluginTemplateUser#isOnline()] returns `true`,
    /// the entry lives forever (until explicit eviction via
    /// [PluginTemplateUserServiceInternal#discardUser]); otherwise the
    /// standard 15-minute access-window applies.
    ///
    /// @implNote TTL values are in nanoseconds as required by the Caffeine
    ///           API. `Long.MAX_VALUE` nanoseconds is effectively infinite
    ///           (~292 years).
    private static final class OnlineAwareExpiry implements Expiry<UUID, PluginTemplateUser> {

        private static final long NEVER_EXPIRE_NANOS = Long.MAX_VALUE;
        private static final long OFFLINE_EXPIRE_NANOS = TimeUnit.MINUTES.toNanos(15L);

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
            return user.isOnline() ? NEVER_EXPIRE_NANOS : OFFLINE_EXPIRE_NANOS;
        }
    }
}

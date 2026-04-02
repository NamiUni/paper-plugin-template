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
import java.util.function.BooleanSupplier;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.identity.Identified;
import net.kyori.adventure.identity.Identity;
import org.jspecify.annotations.NullMarked;

/// Application service for managing player [UserProfile] data.
///
/// ## Dual-cache architecture
///
/// Two Caffeine caches work in tandem to eliminate repository round-trips during
/// normal gameplay while bounding memory use.
///
/// ### Pre-load cache (`preloadCache`)
///
/// A short-lived `Cache<UUID, UserProfile>` populated during
/// `AsyncPlayerConnectionConfigureEvent`, before a `Player` object exists.
/// Entries expire 30 seconds after write regardless of access. Its sole purpose
/// is to bridge the gap between the async connection event and the first
/// [#loadUser] call on `PlayerJoinEvent`. Once consumed, the preloaded profile
/// is promoted into `userCache` and the preload entry expires naturally — no
/// explicit removal is needed.
///
/// ### User cache (`userCache`)
///
/// A `Cache<UUID, PluginTemplateUser>` governed by [OnlineAwareExpiry]:
///
/// - **Online players** (`isOnline() == true`): pinned indefinitely. The only
///   removal path is explicit eviction via [#discardUser] on disconnect.
/// - **Offline players** (`isOnline() == false`): evicted 15 minutes after
///   the last cache interaction, preventing unbounded memory growth for offline
///   lookups.
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
    private final Cache<UUID, UserProfile> preloadCache;
    private final Cache<UUID, PluginTemplateUser> userCache;

    @Inject
    private PluginTemplateUserServiceInternal(final UserRepository repository) {
        this.repository = repository;
        this.preloadCache = Caffeine.newBuilder()
                .maximumSize(512L)
                .expireAfterWrite(30L, TimeUnit.SECONDS)
                .build();
        this.userCache = Caffeine.newBuilder()
                .maximumSize(512L)
                .expireAfter(new OnlineAwareExpiry())
                .build();
    }

    /// Pre-loads a player's profile into the pre-load cache so that the
    /// subsequent [#loadUser] call can resolve without a repository round-trip.
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
    ///         [Optional#empty()] for first-time players; may complete exceptionally
    ///         if repository I/O fails
    /// @apiNote This method is intentionally internal. Callers outside this
    ///          service should use [#loadUser] instead.
    ///
    /// @implNote Completes on a virtual-thread executor for the repository path;
    ///           completes on the calling thread for cache-hit paths.
    public CompletableFuture<Optional<UserProfile>> loadUserProfile(final UUID uuid) {
        final PluginTemplateUser cached = this.userCache.getIfPresent(uuid);
        if (cached instanceof final PlatformUser<?> platformUser) {
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

    /// Persists the current state of `user` to storage without modifying any
    /// system-managed fields.
    ///
    /// Before writing, the in-memory cache entry is refreshed so that the
    /// [OnlineAwareExpiry] policy re-evaluates the player's online status and
    /// recalculates the TTL from the current moment.
    ///
    /// Suitable for saving user-configurable fields changed by a command,
    /// including offline-player edits. For disconnect and world-save
    /// checkpoints, use [#persistOnlinePlayer] instead, which additionally
    /// stamps `lastSeen`.
    ///
    /// @param user the user whose current profile state should be saved
    /// @return a future that completes when the repository write finishes, or
    ///         completes immediately if `user` is not a recognized internal type
    /// @apiNote If `user` is not an instance of [PlatformUser] — for example
    ///          when mocked in tests — this method returns a completed future
    ///          immediately without any side effects.
    public CompletableFuture<Void> upsertUser(final PluginTemplateUser user) {
        if (user instanceof final PlatformUser<?> platformUser) {
            final UserProfile current = platformUser.profile();
            // Re-put to trigger expireAfterUpdate so the expiry policy
            // re-evaluates isOnline() and recalculates the TTL.
            this.userCache.put(current.uuid(), platformUser);
            return this.repository.upsert(current);
        }
        return CompletableFuture.completedFuture(null);
    }

    /// Stamps [UserProfile#lastSeen()] with the current instant, persists the
    /// updated profile, and then evicts the cache entry.
    ///
    /// This method encodes the **online-player lifecycle checkpoint** as an
    /// explicit, named operation. The `lastSeen` timestamp is updated here and
    /// nowhere else, ensuring it always reflects the actual disconnect or save
    /// time rather than an arbitrary write.
    ///
    /// Cache eviction is guaranteed via [CompletableFuture#whenComplete], so
    /// the entry is removed regardless of whether the repository write succeeds
    /// or fails. This prevents stale data from accumulating in the user cache.
    ///
    /// @apiNote If `user` is not an instance of [PlatformUser], this method
    ///          returns a completed future immediately. This is intentional and avoids
    ///          errors when called with mock objects in tests.
    ///
    /// @param user the online player whose session is ending or being
    ///             checkpointed
    /// @return a future that completes when the repository write finishes; the
    ///         cache entry is evicted unconditionally after completion
    public CompletableFuture<Void> persistOnlinePlayer(final PluginTemplateUser user) {
        if (user instanceof final PlatformUser<?> platformUser) {
            platformUser.updateProfile(profile -> profile.withLastSeen(Instant.now()));
            final UserProfile updated = platformUser.profile();
            return this.repository.upsert(updated)
                    .whenComplete((_, _) -> this.discardUser(updated.uuid()));
        }
        return CompletableFuture.completedFuture(null);
    }

    /// Evicts the entry for `uuid` from the user cache.
    ///
    /// Calling this method while the player is still online removes them from
    /// the pinned-forever expiry bucket; a subsequent cache miss will rebuild
    /// their entry from the repository.
    ///
    /// This method is idempotent: calling it multiple times for the same `uuid`
    /// is safe and has no additional effect after the first call.
    ///
    /// @param uuid the player UUID to evict
    public void discardUser(final UUID uuid) {
        this.userCache.invalidate(uuid);
    }

    @Override
    public <P extends Audience & Identified> Optional<PluginTemplateUser> getUser(final P player) {
        return Optional.ofNullable(
                this.userCache.getIfPresent(player.get(Identity.UUID).orElseThrow()));
    }

    @Override
    public <P extends Audience & Identified> CompletableFuture<PluginTemplateUser> loadUser(
            final P player, final BooleanSupplier onlineCheck) {
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
            final UserProfile resolved = new UserProfile(uuid, currentName, preloaded.lastSeen());
            final PlatformUser<P> platformUser = new PlatformUser<>(player, resolved, onlineCheck);
            this.userCache.put(uuid, platformUser);
            return CompletableFuture.completedFuture(platformUser);
        }

        // 3. Repository — async I/O; new players receive a default profile.
        return this.repository.findById(uuid)
                .thenApply(existing -> existing
                        .map(profile -> new UserProfile(profile.uuid(), currentName, profile.lastSeen()))
                        .orElse(new UserProfile(uuid, currentName, Instant.now())))
                .thenApply(profile -> {
                    final PlatformUser<P> platformUser = new PlatformUser<>(player, profile, onlineCheck);
                    this.userCache.put(uuid, platformUser);
                    return platformUser;
                });
    }

    @Override
    public CompletableFuture<Void> deleteUser(final UUID uuid) {
        this.userCache.invalidate(uuid);
        this.preloadCache.invalidate(uuid);
        return this.repository.delete(uuid);
    }

    // -------------------------------------------------------------------------
    // Cache expiry policy
    // -------------------------------------------------------------------------

    /// Caffeine [Expiry] policy that pins online players indefinitely in cache
    /// and applies 15-minute access-based expiry to offline players.
    ///
    /// All three lifecycle callbacks — [#expireAfterCreate],
    /// [#expireAfterUpdate], and [#expireAfterRead] — delegate to the same
    /// [#ttl] helper. If [PluginTemplateUser#isOnline()] returns `true`, the
    /// entry lives forever (until explicit eviction via [#discardUser]);
    /// otherwise the standard 15-minute access-window applies.
    ///
    /// @implNote TTL values are in nanoseconds as required by the Caffeine API.
    ///           `Long.MAX_VALUE` nanoseconds is effectively infinite (~292 years).
    private static final class OnlineAwareExpiry implements Expiry<UUID, PluginTemplateUser> {

        private static final long NEVER_EXPIRE_NANOS = Long.MAX_VALUE;
        private static final long OFFLINE_EXPIRE_NANOS = TimeUnit.MINUTES.toNanos(15L);

        @Override
        public long expireAfterCreate(
                final UUID key, final PluginTemplateUser user, final long currentTime) {
            return this.ttl(user);
        }

        @Override
        public long expireAfterUpdate(
                final UUID key, final PluginTemplateUser user,
                final long currentTime, final long currentDuration) {
            return this.ttl(user);
        }

        @Override
        public long expireAfterRead(
                final UUID key, final PluginTemplateUser user,
                final long currentTime, final long currentDuration) {
            return this.ttl(user);
        }

        private long ttl(final PluginTemplateUser user) {
            return user.isOnline() ? NEVER_EXPIRE_NANOS : OFFLINE_EXPIRE_NANOS;
        }
    }
}

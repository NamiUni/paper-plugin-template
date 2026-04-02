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
/// ## Cache architecture
///
/// Two caches work in tandem:
///
/// ### Pre-load cache (`preloadCache`)
///
/// A short-lived `Cache<UUID, UserProfile>` populated during
/// [io.papermc.paper.event.connection.configuration.AsyncPlayerConnectionConfigureEvent],
/// before a `Player` object exists. Entries expire 30 seconds after write regardless
/// of access. The sole purpose of this cache is to bridge the gap between the async
/// connection event (where only a UUID is available) and the first [#loadUser] call
/// (where a live player reference arrives). Once consumed by [#loadUser], the
/// preloaded profile is promoted into `userCache` and the preload entry expires
/// naturally.
///
/// ### User cache (`userCache`)
///
/// A `Cache<UUID, PluginTemplateUser>` governed by [OnlineAwareExpiry]:
///
/// - **Online players** (`isOnline() == true`): pinned indefinitely.
///   Explicit eviction via [#discardUser] on disconnect is the only removal path.
/// - **Offline players** (`isOnline() == false`): evicted 15 minutes after the
///   last cache interaction, matching the original access-based expiry behavior.
///
/// This dual-cache strategy eliminates involuntary eviction of online players
/// while preserving bounded memory use for offline lookups.
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

    /// Pre-loads a player's profile into the [#preloadCache] so that the
    /// subsequent [#loadUser] call can resolve without a repository round-trip.
    ///
    /// If the profile is already present in [#userCache] (e.g. the player
    /// reconnects quickly), the cached value is returned immediately.
    ///
    /// Called during [io.papermc.paper.event.connection.configuration.AsyncPlayerConnectionConfigureEvent],
    /// where no live [org.bukkit.entity.Player] object exists yet.
    ///
    /// @param uuid the connecting player's UUID
    /// @return a future resolving to the profile, or [Optional#empty()] for new players
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

    /// Persists the current state of `user` to storage **without modifying any
    /// system-managed fields**.
    ///
    /// The in-memory cache entry is refreshed before the write so that the expiry
    /// policy re-evaluates the player's online status.
    ///
    /// Suitable for saving user-configurable fields changed by a command, including
    /// offline player edits. For disconnect and world-save events, use
    /// [#persistOnlinePlayer] instead.
    ///
    /// @param user the user whose current profile state should be saved
    /// @return a future that completes when the write finishes, or immediately
    ///         if `user` is not a recognized internal type
    public CompletableFuture<Void> upsertUser(final PluginTemplateUser user) {
        if (user instanceof final PlatformUser<?> platformUser) {
            final UserProfile current = platformUser.profile();
            // Re-put to trigger expireAfterUpdate so that the expiry policy
            // re-evaluates isOnline() and recalculates the TTL.
            this.userCache.put(current.uuid(), platformUser);
            return this.repository.upsert(current);
        }
        return CompletableFuture.completedFuture(null);
    }

    /// Stamps [UserProfile#lastSeen()] with the current instant, persists the
    /// profile, then evicts the cache entry.
    ///
    /// This method encodes the **online-player disconnect lifecycle** as an
    /// explicit, named operation. The `lastSeen` timestamp is updated here and
    /// nowhere else, ensuring it always reflects the time the player last left
    /// the server rather than the time of an arbitrary save.
    ///
    /// Cache eviction is guaranteed via
    /// [java.util.concurrent.CompletableFuture#whenComplete], regardless of
    /// whether the repository write succeeds or fails.
    ///
    /// @param user the online player who is disconnecting or whose session is
    ///             being checkpointed (e.g. world save)
    /// @return a future that completes when the write finishes
    public CompletableFuture<Void> persistOnlinePlayer(final PluginTemplateUser user) {
        if (user instanceof final PlatformUser<?> platformUser) {
            platformUser.updateProfile(profile -> profile.withLastSeen(Instant.now()));
            final UserProfile updated = platformUser.profile();
            return this.repository.upsert(updated)
                    .whenComplete((_, __) -> this.discardUser(updated.uuid()));
        }
        return CompletableFuture.completedFuture(null);
    }

    /// Evicts the profile for `uuid` from both caches.
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
    /// and applies a 15-minute access-based expiry to offline players.
    ///
    /// [#expireAfterCreate], [#expireAfterUpdate], and [#expireAfterRead] all
    /// delegate to the same logic: if [PluginTemplateUser#isOnline()] returns
    /// `true`, the entry lives forever (until explicit eviction via
    /// [PluginTemplateUserServiceInternal#discardUser]); otherwise the standard
    /// access-based 15-minute window applies.
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

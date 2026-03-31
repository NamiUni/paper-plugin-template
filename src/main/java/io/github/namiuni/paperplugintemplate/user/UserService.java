/*
 * paper-plugin-template
 *
 * Copyright (c) 2026. Namiu/Unitarou
 * Contributors []
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
package io.github.namiuni.paperplugintemplate.user;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.namiuni.paperplugintemplate.user.storage.UserProfile;
import io.github.namiuni.paperplugintemplate.user.storage.UserRepository;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/// Application service for managing player [UserProfile] data.
///
/// Maintains an in-memory cache of online or recently active players to provide
/// strictly non-blocking reads. Cache misses and write operations are delegated
/// asynchronously to the injected [UserRepository].
///
/// Futures returned by write or load operations complete on the executor owned
/// by the active repository implementation.
///
/// @see UserRepository
@Singleton
@NullMarked
public final class UserService {

    private final UserRepository repository;
    private final Cache<UUID, UserProfile> cache;

    /// Constructs a new `UserService` and initializes a bounded in-memory cache.
    ///
    /// @param repository the active storage backend, selected at startup by
    ///                   [io.github.namiuni.paperplugintemplate.user.storage.StorageModule]
    @Inject
    private UserService(final UserRepository repository) {
        this.repository = repository;
        this.cache = Caffeine.newBuilder()
                .maximumSize(512L)
                .expireAfterAccess(15, TimeUnit.MINUTES)
                .build();
    }

    // -------------------------------------------------------------------------
    // Queries
    // -------------------------------------------------------------------------

    /// Retrieves the profile for `uuid` immediately from the in-memory cache.
    ///
    /// This method is strictly non-blocking and does not query the underlying storage.
    /// If the player's data has not been loaded via [#loadUser] or has expired,
    /// this will return [Optional#empty()].
    ///
    /// @param uuid the player UUID to look up
    /// @return an [Optional] containing the cached profile, or empty if not loaded
    public Optional<UserProfile> getUser(final UUID uuid) {
        return Optional.ofNullable(this.cache.getIfPresent(uuid));
    }

    // -------------------------------------------------------------------------
    // Commands
    // -------------------------------------------------------------------------

    /// Loads the profile for `uuid`. Checks the in-memory cache first to avoid
    /// redundant I/O; if absent, fetches from storage or creates a new record.
    ///
    /// If a profile is fetched from storage and the player's current name differs
    /// from the stored one, the profile is updated and persisted before the future
    /// resolves. The resolved value is always the authoritative post-load profile
    /// which is also placed into the local cache.
    ///
    /// @param uuid the player UUID to look up
    /// @param name the player's current username; `null` falls back to `"Unknown"`
    /// @return a future resolving to the loaded or newly created [UserProfile]
    public CompletableFuture<UserProfile> loadUser(final UUID uuid, final @Nullable String name) {
        final String resolvedName = Objects.requireNonNullElse(name, "Unknown");

        final Optional<UserProfile> cached = this.getUser(uuid);
        return cached.map(CompletableFuture::completedFuture)
                .orElseGet(() -> this.repository.findById(uuid)
                        .thenCompose(existing -> {
                            if (existing.isPresent()) {
                                final UserProfile found = existing.get();
                                if (found.name().equals(resolvedName)) {
                                    // Name unchanged: cache only, no write.
                                    this.cache.put(uuid, found);
                                    return CompletableFuture.completedFuture(found);
                                }
                                // Name changed: update cache and persist.
                                final UserProfile updated = new UserProfile(uuid, resolvedName, found.lastSeen());
                                this.cache.put(uuid, updated);
                                return this.repository.upsert(updated).thenApply(_ -> updated);
                            }
                            // New player: create, cache, and persist.
                            final UserProfile created = new UserProfile(uuid, resolvedName, Instant.now());
                            this.cache.put(uuid, created);
                            return this.repository.upsert(created).thenApply(_ -> created);
                        }));

    }

    /// Updates the [UserProfile#lastSeen()] timestamp to now and persists the
    /// result to storage.
    ///
    /// If the UUID is not present in the cache (e.g. the player was never loaded)
    /// the future completes immediately without any I/O.
    ///
    /// @param uuid the player UUID whose profile should be saved
    /// @return a future that completes when the updated profile has been committed
    ///         to storage, or immediately if the UUID was not cached
    public CompletableFuture<Void> saveUser(final UUID uuid) {
        final UserProfile current = this.cache.getIfPresent(uuid);
        if (current == null) {
            return CompletableFuture.completedFuture(null);
        }
        final UserProfile updated = new UserProfile(current.uuid(), current.name(), Instant.now());
        this.cache.put(uuid, updated);
        return this.repository.upsert(updated);
    }

    /// Evicts the given UUID from the in-memory cache without touching storage.
    ///
    /// Call this after [#saveUser] completes during a disconnect sequence. Since
    /// [#saveUser] returns a `CompletableFuture`, the eviction is safe to perform
    /// immediately after firing the save because `saveUser` captures the profile
    /// reference before returning.
    ///
    /// @param uuid the player UUID to evict
    public void discardUser(final UUID uuid) {
        this.cache.invalidate(uuid);
    }

    /// Removes all persisted data for the player identified by `userProfile.uuid()`.
    ///
    /// No-op if no record exists.
    ///
    /// @param uuid the player UUID to delete
    /// @return a future that completes when the record has been removed from storage
    public CompletableFuture<Void> deleteUser(final UUID uuid) {
        this.cache.invalidate(uuid);
        return this.repository.delete(uuid);
    }
}

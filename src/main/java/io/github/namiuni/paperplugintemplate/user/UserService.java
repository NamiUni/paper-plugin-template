/*
 * paper-plugin-template
 *
 * Copyright (c) 2026. Namiu/Unitarou
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
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/// Application service for managing player [UserProfile] data.
///
/// Delegates all persistence to the injected [UserRepository]. This service
/// does not maintain its own cache; caching is the responsibility of the
/// active repository implementation.
///
/// All returned futures complete on the executor owned by the repository.
///
/// @see UserRepository
@Singleton
@NullMarked
public final class UserService {

    private final UserRepository repository;
    private final Cache<UUID, UserProfile> cache;

    /// Constructs a new `UserService` and initializes an unbounded in-memory cache.
    ///
    /// @param repository the active storage backend, selected at startup by
    ///                   [io.github.namiuni.paperplugintemplate.user.storage.StorageModule]
    @Inject
    private UserService(final UserRepository repository) {
        this.repository = repository;
        this.cache = Caffeine.newBuilder().build();
    }

    // -------------------------------------------------------------------------
    // Queries
    // -------------------------------------------------------------------------

    /// Returns the stored profile for `uuid`.
    ///
    /// @param uuid the player UUID to look up
    /// @return a future resolving to the profile, or empty if none is stored
    public Optional<UserProfile> getUser(final UUID uuid) {
        return Optional.ofNullable(this.cache.getIfPresent(uuid));
    }

    // -------------------------------------------------------------------------
    // Commands
    // -------------------------------------------------------------------------

    /// Returns the stored profile for `uuid`, or syntheses a new one if absent.
    ///
    /// @param uuid the player UUID to look up
    /// @param name the player's current username; `null` falls back to `"Unknown"`
    /// @return a future resolving to the existing or synthesized profile
    public CompletableFuture<Void> loadUser(final UUID uuid, final @Nullable String name) {
        final String resolvedName = Objects.requireNonNullElse(name, "Unknown");
        return this.repository.findById(uuid)
                .thenCompose(existing -> {
                    if (existing.isPresent()) {
                        final UserProfile found = existing.get();
                        if (found.name().equals(resolvedName)) {
                            // Name unchanged: cache only, no write.
                            this.cache.put(uuid, found);
                            return CompletableFuture.completedFuture(null);
                        }
                        // Name changed: update cache and persist.
                        final UserProfile updated = new UserProfile(uuid, resolvedName, found.lastSeen());
                        this.cache.put(uuid, updated);
                        return this.repository.upsert(updated);
                    }
                    // New player: create, cache, and persist.
                    final UserProfile created = new UserProfile(uuid, resolvedName, Instant.now());
                    this.cache.put(uuid, created);
                    return this.repository.upsert(created);
                });
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

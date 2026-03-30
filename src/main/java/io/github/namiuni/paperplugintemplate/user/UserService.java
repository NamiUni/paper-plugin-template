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

/// Application-layer service managing the lifecycle and in-memory cache of
/// [UserProfile] instances.
///
/// ## Layered caching
///
/// `UserService` sits directly above [UserRepository] and maintains a
/// synchronous Caffeine [Cache] keyed by player [UUID]. All reads served from
/// the cache are non-blocking and avoid database round-trips; repository I/O is
/// only triggered during [#loadUser] (on connection) and [#saveUser] / [#deleteUser]
/// (on disconnect or administrative removal).
///
/// Callers must not infer state from commands; use [#getUser] to read the
/// cached profile after a command completes.
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

    /// Returns the cached [UserProfile] for the given UUID, if present.
    ///
    /// This method is purely a cache read; it never performs any I/O. A profile
    /// is guaranteed to be present for any player who has passed through
    /// [#loadUser] and has not yet been evicted via [#discardUser] or
    /// [#deleteUser].
    ///
    /// @param uuid the player UUID to look up
    /// @return an [Optional] containing the cached profile, or empty if the UUID
    ///         is not present in the cache
    public Optional<UserProfile> getUser(final UUID uuid) {
        return Optional.ofNullable(this.cache.getIfPresent(uuid));
    }

    // -------------------------------------------------------------------------
    // Commands
    // -------------------------------------------------------------------------

    /// Loads a user's data from the repository, creating a new record if none exists,
    /// then populates the in-memory cache.
    ///
    /// This method is intended to be called during
    /// [io.papermc.paper.event.connection.configuration.AsyncPlayerConnectionConfigureEvent]
    /// with `.join()` to ensure the cache is warm before the play phase begins.
    ///
    /// Behavior on each connection:
    ///
    ///   - **Existing player, same name** — profile is loaded and cached; no write occurs.
    ///   - **Existing player, name changed** — cached profile is updated with the new name
    ///     and persisted to storage.
    ///   - **New player** — a fresh [UserProfile] is created, cached, and persisted.
    ///
    /// @param uuid the player UUID obtained from the connection profile
    /// @param name the player's current display name; `null` is treated as `"Unknown"`
    /// @return a future that completes when the cache has been populated and any
    ///         necessary write has been committed to storage
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

    /// Permanently removes the given UUID from both the cache and storage.
    ///
    /// This is an administrative operation intended for data-deletion commands, not
    /// for the normal disconnect flow (use [#saveUser] followed by [#discardUser]
    /// for disconnects).
    ///
    /// @param uuid the player UUID to delete
    /// @return a future that completes when the record has been removed from storage
    public CompletableFuture<Void> deleteUser(final UUID uuid) {
        this.cache.invalidate(uuid);
        return this.repository.delete(uuid);
    }
}

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
package io.github.namiuni.paperplugintemplate.user.storage;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.jspecify.annotations.NullMarked;

/// Storage-agnostic data access contract for [UserProfile].
///
/// Implementations must be thread-safe. The active implementation is selected at
/// startup by [io.github.namiuni.paperplugintemplate.user.storage.StorageModule]
/// based on the configured [StorageType].
///
/// Callers should not invoke this interface directly; prefer
/// [io.github.namiuni.paperplugintemplate.user.UserService], which layers an
/// in-memory Caffeine cache on top of any repository implementation.
@NullMarked
public interface UserRepository {

    /// Retrieves a user by their unique identifier.
    ///
    /// @param uuid the player UUID to look up
    /// @return a future that completes with the matching profile wrapped in
    ///         [Optional], or [Optional#empty()] if no record exists for the given UUID
    CompletableFuture<Optional<UserProfile>> findById(UUID uuid);

    /// Persists the given user data, inserting a new record or updating the existing one.
    ///
    /// This operation must be idempotent: calling it multiple times with the same
    /// [UserProfile#uuid()] must not produce duplicate rows.
    ///
    /// @param userProfile the user data to persist
    /// @return a future that completes with `null` when the write has been committed
    CompletableFuture<Void> upsert(UserProfile userProfile);

    /// Removes all persisted data for the given player UUID.
    ///
    /// No-op if the UUID is not present in storage.
    ///
    /// @param uuid the player UUID whose record should be removed
    /// @return a future that completes with `null` when the deletion has been committed
    CompletableFuture<Void> delete(UUID uuid);

    /// Initializes the underlying storage backend (e.g. creates tables or directories).
    ///
    /// Called once at plugin startup before any other method on this repository is invoked.
    void initialize();
}

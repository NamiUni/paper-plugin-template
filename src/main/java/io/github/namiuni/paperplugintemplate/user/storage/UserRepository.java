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
/// All methods are non-blocking and return [CompletableFuture]; implementations
/// must be thread-safe. The active implementation is selected at startup based on
/// the configured [StorageType].
///
/// Callers should prefer [io.github.namiuni.paperplugintemplate.user.UserService]
/// over using this interface directly.
@NullMarked
public interface UserRepository {

    /// Returns the profile for `uuid`.
    ///
    /// @param uuid the player UUID to look up
    /// @return a future resolving to the profile, or [Optional#empty()] if absent
    CompletableFuture<Optional<UserProfile>> findById(UUID uuid);

    /// Persists `userProfile`, inserting or updating as necessary.
    ///
    /// This operation must be idempotent: repeated calls with the same
    /// [UserProfile#uuid()] must not produce duplicate rows.
    ///
    /// @param userProfile the profile to persist
    /// @return a future that completes when the write finishes
    CompletableFuture<Void> upsert(UserProfile userProfile);

    /// Removes all persisted data for `uuid`.
    ///
    /// No-op if `uuid` is not present in storage.
    ///
    /// @param uuid the player UUID to remove
    /// @return a future that completes when the deletion finishes
    CompletableFuture<Void> delete(UUID uuid);

    /// Initializes the underlying storage (e.g. creates tables or directories).
    ///
    /// Called once at plugin startup before any other method on this repository.
    void initialize();
}

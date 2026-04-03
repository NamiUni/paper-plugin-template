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
package io.github.namiuni.paperplugintemplate.common.user.storage;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.jspecify.annotations.NullMarked;

/// Storage-agnostic data access contract for [UserProfile].
///
/// All read and write operations are non-blocking and return
/// [CompletableFuture]s that complete on a virtual-thread executor managed
/// by the implementation. Callers must never block the main server thread
/// on these futures.
///
/// The active implementation is selected at startup based on the configured
/// [StorageType] and is injected by the Guice `StorageModule`.
///
/// @apiNote Callers should prefer the user service over using this
///          interface directly. Direct use bypasses the in-memory cache
///          and may cause redundant I/O.
/// @implSpec All implementations must be thread-safe and must complete the
///           returned futures on a virtual-thread or I/O-thread executor
///           rather than on the caller's thread. Blocking the Paper main
///           thread is prohibited.
@NullMarked
public interface UserRepository {

    /// Returns the persisted profile for the given `uuid`.
    ///
    /// @param uuid the player UUID to look up
    /// @return a future resolving to the profile wrapped in [Optional], or
    ///         [Optional#empty()] if no record exists for this UUID; may
    ///         complete exceptionally if the underlying storage is
    ///         unreachable
    CompletableFuture<Optional<UserProfile>> findById(UUID uuid);

    /// Persists `userProfile`, inserting a new row or updating an existing
    /// one as necessary.
    ///
    /// This operation must be idempotent: repeated calls with the same
    /// [UserProfile#uuid()] must not produce duplicate rows.
    ///
    /// @param userProfile the profile to persist; must not be `null`
    /// @return a future that completes with `null` when the write finishes;
    ///         may complete exceptionally if the storage write fails
    /// @implSpec Implementations must treat this operation as an upsert. A
    ///           pure insert that fails on a duplicate key is not acceptable.
    CompletableFuture<Void> upsert(UserProfile userProfile);

    /// Removes all persisted data for `uuid`.
    ///
    /// If no row exists for the given UUID, this method completes
    /// successfully without error — it is a no-op in the absence of data.
    ///
    /// @param uuid the player UUID whose record should be deleted
    /// @return a future that completes with `null` when the deletion
    ///         finishes; may complete exceptionally if the storage operation
    ///         fails
    CompletableFuture<Void> delete(UUID uuid);

    /// Initializes the underlying storage backend.
    ///
    /// For SQL backends this creates the required tables if they do not
    /// already exist. For file-based backends this creates the storage
    /// directories. Called exactly once at plugin startup, before any other
    /// method on this repository.
    ///
    /// @throws java.io.UncheckedIOException if the storage cannot be
    ///         initialized (e.g. directory creation fails for the JSON
    ///         backend)
    void initialize();
}

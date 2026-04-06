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
package io.github.namiuni.paperplugintemplate.api.user;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.identity.Identified;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NullMarked;

/// Service for accessing and managing [PluginTemplateUser] instances.
///
/// ## Cache resolution order
///
/// [#loadUser] resolves through three tiers in order:
///
/// 1. **In-memory user cache** — non-blocking, always preferred.
/// 2. **Connection preload cache** — populated before the `Player` object
///    exists; also non-blocking.
/// 3. **Repository** — async I/O; only reached on a cold-cache miss.
///    New players receive a default profile.
///
/// ## Online-status contract
///
/// The `onlineCheck` supplier passed to [#loadUser] is evaluated by the
/// cache's expiry policy on **every** cache interaction after the initial
/// insertion. Online players (`onlineCheck.getAsBoolean() == true`) are
/// pinned indefinitely; offline players are evicted 15 minutes after their
/// last cache access.
///
/// ## Thread safety
///
/// All methods on this service are safe to call from any thread, including
/// Paper's async event threads and virtual threads.
///
/// @apiNote On the Paper platform, always pass `player::isOnline` as the
///          `onlineCheck` argument. For offline-player operations such as admin
///          commands, pass `() -> false`.
@NullMarked
@ApiStatus.NonExtendable
public interface PluginTemplateUserService {

    /// Returns the cached [PluginTemplateUser] for `uuid`, if present.
    ///
    /// This method never blocks and never triggers a repository lookup.
    /// Returns [Optional#empty()] if the uuid has not yet been loaded or
    /// has already been evicted from the cache.
    ///
    /// @param uuid the player uuid whose cached entry is requested
    /// @return the cached user wrapped in [Optional], or [Optional#empty()]
    ///         on a cache miss
    Optional<PluginTemplateUser> getUser(UUID uuid);

    /// Returns the [PluginTemplateUser] for `player`, loading from the
    /// repository if no cache entry exists.
    ///
    /// See the class-level documentation for the full three-tier resolution
    /// order. The resolved entry's cache expiry is governed entirely by
    /// [PluginTemplateUser#isOnline()]: a `true` result pins the entry
    /// indefinitely; `false` allows expiry 15 minutes after the last cache
    /// access.
    ///
    /// Because online status is now read directly from the platform player
    /// object inside the adapter (e.g. via
    /// `org.bukkit.entity.Player#isOnline()`), callers no longer need
    /// to supply an `onlineCheck` supplier.
    ///
    /// @param <P>    the platform player type; must extend both [Audience] and [Identified]
    /// @param player the player to load; must not be `null`
    /// @return a future resolving to the user; may complete exceptionally if
    ///         the repository is unreachable on a cold cache miss
    /// @implNote The returned future completes on a virtual-thread executor
    ///           for the repository path and on the calling thread for
    ///           cache-hit paths. Callers must not assume a specific
    ///           completion thread.
    <P extends Audience & Identified> CompletableFuture<PluginTemplateUser> loadUser(P player);

    /// Permanently removes all data for `uuid` from both caches and the
    /// underlying storage backend.
    ///
    /// Intended for administrative actions such as GDPR data deletion and
    /// **not** for routine disconnect handling. Cache eviction on normal
    /// disconnects is performed automatically by the service as part of the
    /// player logout sequence.
    ///
    /// @param uuid the player UUID whose data should be permanently deleted
    /// @return a future that completes when the deletion finishes; may complete
    ///         exceptionally if the repository write fails
    CompletableFuture<Void> deleteUser(UUID uuid);
}

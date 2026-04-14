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
package io.github.namiuni.paperplugintemplate.common.component.components;

import java.time.Instant;
import net.kyori.adventure.audience.Audience;
import org.jspecify.annotations.NullMarked;

/// ECS component that carries a platform-specific player handle and its
/// associated live-data accessors.
///
/// Implementations hold a reference to the underlying platform player object
/// (e.g. `org.bukkit.entity.Player`) and expose the subset of live data
/// required by the `common` service layer, keeping platform-specific imports
/// confined to their respective adapter modules.
///
/// ## Immutability contract
///
/// Per the [Component] contract, implementations must be effectively immutable.
/// The player reference may reflect changing observable state (online status,
/// last-seen time), but the component stored in the
/// [io.github.namiuni.paperplugintemplate.common.component.ComponentStore]
/// must not be replaced without an explicit
/// [io.github.namiuni.paperplugintemplate.common.component.ComponentStore#set] call.
///
/// ## Thread safety
///
/// All methods must be safe to call from any thread. On the Paper platform this
/// contract is satisfied by delegating to `Player` accessors that Paper guarantees
/// are thread-safe.
@NullMarked
public interface PlayerComponent extends Component {

    /// Returns the [Audience] used to deliver Adventure messages to this player.
    ///
    /// @return the player's audience, never `null`
    Audience audience();

    /// Returns `true` if the underlying platform player is currently connected
    /// to the server.
    ///
    /// This method is evaluated by the Caffeine cache expiry policy on every
    /// cache interaction. Online players are pinned indefinitely; offline players
    /// expire after the configured window following their last cache access.
    ///
    /// @return `true` if the player is connected to the server
    boolean isOnline();

    /// Returns the instant at which this player was last active on the server.
    ///
    /// The source of truth is platform-defined. On Paper, this value is sourced
    /// from `Player#getLastSeen()`.
    ///
    /// @return the last-seen timestamp, never `null`
    Instant lastSeen();
}

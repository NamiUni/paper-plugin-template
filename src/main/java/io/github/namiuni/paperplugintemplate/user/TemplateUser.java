/*
 * paper-plugin-template
 *
 * Copyright (c) 2025. Namiu/Unitarou
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

import java.util.Optional;
import java.util.UUID;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.ForwardingAudience;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

/// Domain model representing a player tracked by this plugin.
///
/// Identified solely by [UUID] so that the record remains valid across
/// reconnections and does not hold a strong reference to the online [Player]
/// object. Implements [ForwardingAudience.Single] so that Adventure messages
/// can be sent directly to a `TemplateUser` regardless of whether the underlying
/// player is currently online; messages sent while the player is offline are silently
/// discarded via [Audience#empty()].
///
/// @param uuid the unique identifier that permanently identifies this player
@NullMarked
public record TemplateUser(UUID uuid) implements ForwardingAudience.Single {

    /// Returns the Adventure [Audience] to which messages are forwarded.
    ///
    /// If the player is currently online the live [Player] instance is
    /// returned; otherwise [Audience#empty()] is returned so that messages are
    /// silently dropped.
    ///
    /// @return the live player audience, or [Audience#empty()] when offline
    @Override
    public Audience audience() {
        return this.player()
                .map(Audience.class::cast)
                .orElse(Audience.empty());
    }

    /// Returns the online [Player] corresponding to this user, if present.
    ///
    /// @return an [Optional] containing the online player, or empty if offline
    public Optional<Player> player() {
        return Optional.ofNullable(Bukkit.getPlayer(this.uuid()));
    }
}

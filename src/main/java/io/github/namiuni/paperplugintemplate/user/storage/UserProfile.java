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

import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.NullMarked;

/// Persistent data record for a player tracked by this plugin.
///
/// Stored in the configured backend (H2, MySQL, or JSON) and cached in memory by
/// [io.github.namiuni.paperplugintemplate.user.UserService]. All components are
/// immutable; to reflect a change create a new instance via the canonical constructor.
///
/// Timestamps are stored as epoch milliseconds (`BIGINT`) to maintain identical
/// semantics across H2 and MySQL without relying on database-specific `TIMESTAMP`
/// behaviour.
///
/// @param uuid     the permanent unique identifier for this player
/// @param name     the most recently observed display name; updated on each connection
///                 if the player has renamed since their last session
/// @param lastSeen the timestamp of the player's most recent disconnect event,
///                 or the time of first registration for brand-new players
@NullMarked
public record UserProfile(
        UUID uuid,
        String name,
        Instant lastSeen
) {
}

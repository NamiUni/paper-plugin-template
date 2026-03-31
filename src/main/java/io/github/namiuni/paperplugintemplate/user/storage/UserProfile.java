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

/// Persistent data for a tracked player.
///
/// Stored and retrieved by [UserRepository]; consumed by [io.github.namiuni.paperplugintemplate.user.UserService]
/// as the unit of I/O between the service layer and the storage backend.
///
/// @param uuid     the player's permanent unique identifier
/// @param name     the player's last-known username
/// @param lastSeen the instant this record was last written to storage
@NullMarked
public record UserProfile(
        UUID uuid,
        String name,
        Instant lastSeen
) {
}

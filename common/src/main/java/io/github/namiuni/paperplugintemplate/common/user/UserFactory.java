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
package io.github.namiuni.paperplugintemplate.common.user;

import io.github.namiuni.paperplugintemplate.api.user.PluginTemplateUser;
import java.time.Instant;
import java.util.UUID;
import net.kyori.adventure.audience.Audience;
import org.jspecify.annotations.NullMarked;

@NullMarked
@FunctionalInterface
public interface UserFactory {

    UserInternal createUser(Audience audience, UUID uuid, String name, Instant lastSeen, PluginTemplateUser.Setting setting);

    default UserInternal createUser(
            final UUID uuid,
            final String name,
            final Instant lastSeen,
            final PluginTemplateUser.Setting setting
    ) {
        return this.createUser(Audience.empty(), uuid, name, lastSeen, setting);
    }
}

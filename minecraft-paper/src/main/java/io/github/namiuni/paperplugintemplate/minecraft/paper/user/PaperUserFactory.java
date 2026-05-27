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
package io.github.namiuni.paperplugintemplate.minecraft.paper.user;

import io.github.namiuni.paperplugintemplate.api.user.PluginTemplateUser;
import io.github.namiuni.paperplugintemplate.common.user.UserFactory;
import io.github.namiuni.paperplugintemplate.common.user.UserInternal;
import jakarta.inject.Inject;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import net.kyori.adventure.audience.Audience;
import org.bukkit.Bukkit;
import org.jspecify.annotations.NullMarked;

@NullMarked
public final class PaperUserFactory implements UserFactory {

    @Inject
    PaperUserFactory() {
    }

    @Override
    public UserInternal createUser(final Audience audience, final UUID uuid, final String name, final Instant lastSeen, final PluginTemplateUser.Setting setting) {
        return new PaperUser(audience, uuid, name, lastSeen, setting);
    }

    @Override
    public UserInternal createUser(final UUID uuid, final String name, final Instant lastSeen, final PluginTemplateUser.Setting setting) {
        final Audience audience = Optional
                .<Audience>ofNullable(Bukkit.getPlayer(uuid))
                .orElse(Audience.empty());
        return this.createUser(audience, uuid, name, lastSeen, setting);
    }
}

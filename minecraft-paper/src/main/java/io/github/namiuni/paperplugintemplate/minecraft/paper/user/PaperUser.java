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
import io.github.namiuni.paperplugintemplate.common.user.UserRecord;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.ForwardingAudience;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

@NullMarked
public record PaperUser(Player player, UserRecord userRecord) implements PluginTemplateUser, ForwardingAudience.Single {

    @Override
    public UUID uuid() {
        return this.player.getUniqueId();
    }

    @Override
    public String name() {
        return this.player.getName();
    }

    @Override
    public Component displayName() {
        return this.player.displayName();
    }

    @Override
    public Locale locale() {
        return this.player.locale();
    }

    @Override
    public Instant lastSeen() {
        return Instant.ofEpochMilli(this.player.getLastSeen());
    }

    @Override
    public boolean isOnline() {
        return this.player.isOnline();
    }

    @Override
    public Audience audience() {
        return this.player;
    }

    @Override
    public Identity identity() {
        return this.player.identity();
    }
}

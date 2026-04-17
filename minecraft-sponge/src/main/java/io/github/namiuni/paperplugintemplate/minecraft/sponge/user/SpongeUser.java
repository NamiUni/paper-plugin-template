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
package io.github.namiuni.paperplugintemplate.minecraft.sponge.user;

import io.github.namiuni.paperplugintemplate.api.user.PluginTemplateUser;
import io.github.namiuni.paperplugintemplate.common.infrastructure.storage.UserRecord;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.ForwardingAudience;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import org.jspecify.annotations.NullMarked;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;

@NullMarked
public record SpongeUser(ServerPlayer player, UserRecord userRecord) implements PluginTemplateUser, ForwardingAudience.Single {

    @Override
    public UUID uuid() {
        return this.player.uniqueId();
    }

    @Override
    public String name() {
        return this.player.name();
    }

    @Override
    public Component displayName() {
        return this.player.get(Keys.DISPLAY_NAME).orElseGet(() -> Component.text(this.player.name()));
    }

    @Override
    public Locale locale() {
        return this.player.locale();
    }

    @Override
    public Instant lastSeen() {
        return this.userRecord.lastSeen();
    }

    @Override
    public boolean isOnline() {
        return Sponge.server().player(this.player.uniqueId()).isPresent();
    }

    @Override
    public Audience audience() {
        return this.player;
    }

    @Override
    public Identity identity() {
        return Identity.identity(this.uuid());
    }
}

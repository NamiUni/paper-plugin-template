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
import java.util.Optional;
import java.util.UUID;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.ForwardingAudience;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

@NullMarked
public record PaperUser(UserRecord userRecord) implements PluginTemplateUser, ForwardingAudience.Single {

    public Optional<Player> player() {
        return Optional.ofNullable(Bukkit.getPlayer(this.userRecord.uuid()));
    }

    @Override
    public UUID uuid() {
        return this.userRecord.uuid();
    }

    @Override
    public String name() {
        return this.player()
                .map(Player::getName)
                .orElse(this.userRecord.name());
    }

    @Override
    public Component displayName() {
        return this.player()
                .map(Player::displayName)
                .orElse(Component.text(this.name()));
    }

    @Override
    public Locale locale() {
        return this.player()
                .map(Player::locale)
                .orElse(Locale.US);
    }

    @Override
    public Instant lastSeen() {
        return this.player()
                .map(Player::getLastSeen)
                .map(Instant::ofEpochMilli)
                .orElse(this.userRecord.lastSeen());
    }

    @Override
    public boolean isOnline() {
        return this.player()
                .map(Player::isOnline)
                .orElse(false);
    }

    @Override
    public Audience audience() {
        return this.player()
                .map(Audience.class::cast)
                .orElse(Audience.empty());
    }

    @Override
    public Identity identity() {
        return this.player()
                .map(Player::identity)
                .orElse(Identity.identity(this.uuid()));
    }
}

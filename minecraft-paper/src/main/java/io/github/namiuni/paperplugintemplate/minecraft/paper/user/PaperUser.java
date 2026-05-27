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
import io.github.namiuni.paperplugintemplate.common.user.UserInternal;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.ForwardingAudience;
import net.kyori.adventure.identity.Identified;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class PaperUser implements UserInternal, ForwardingAudience.Single {

    private final Audience audience;
    private final UUID uuid;
    private final String name;
    private final Instant lastSeen;

    private final AtomicReference<Setting> setting;

    public PaperUser(
            final Audience audience,
            final UUID uuid,
            final String name,
            final Instant lastSeen,
            final Setting setting
    ) {
        this.audience = audience;
        this.uuid = uuid;
        this.name = name;
        this.lastSeen = lastSeen;

        this.setting = new AtomicReference<>(setting);
    }

    @Override
    public UUID uuid() {
        return this.audience.getOrDefault(Identity.UUID, this.uuid);
    }

    @Override
    public String name() {
        return this.audience.getOrDefault(Identity.NAME, this.name);
    }

    @Override
    public Component displayName() {
        return this.audience.getOrDefault(Identity.DISPLAY_NAME, Component.text(this.name()));
    }

    @Override
    public Locale locale() {
        return this.audience.getOrDefault(Identity.LOCALE, Locale.US);
    }

    @Override
    public Instant lastSeen() {
        if (this.audience instanceof final Player player) {
            return Instant.ofEpochMilli(player.getLastSeen());
        }

        return this.lastSeen;
    }

    @Override
    public boolean isOnline() {
        if (this.audience instanceof final Player player) {
            return player.isOnline();
        }

        return false;
    }

    @Override
    public Identity identity() {
        if (this.audience instanceof final Identified identified) {
            return identified.identity();
        }

        return Identity.identity(this.uuid());
    }

    @Override
    public Setting getSetting() {
        return this.setting.get();
    }

    @Override
    public Setting editSetting(final UnaryOperator<Setting> current) {
        return this.setting.updateAndGet(current);
    }

    @Override
    public UserInternal withAudience(final Audience audience) {
        return new PaperUser(audience, this.uuid(), this.name(), this.lastSeen(), this.setting.get());
    }

    @Override
    public Audience audience() {
        return this.audience;
    }

    @Override
    public boolean equals(final @Nullable Object other) {
        if (other instanceof final PluginTemplateUser that) {
            if (that == this) {
                return true;
            }

            return Objects.equals(that.uuid(), this.uuid());
        }

        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.uuid(), this.getSetting());
    }

    @Override
    public String toString() {
        return "PaperUser{" +
                "audience=" + this.audience +
                ", uuid=" + this.uuid() +
                ", name=" + this.name() +
                ", displayName=" + this.displayName() +
                ", locale=" + this.locale() +
                ", lastSeen=" + this.lastSeen() +
                ", isOnline=" + this.isOnline() +
                '}';
    }
}

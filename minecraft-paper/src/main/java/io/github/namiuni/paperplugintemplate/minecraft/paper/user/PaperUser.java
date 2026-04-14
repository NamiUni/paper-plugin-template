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

import io.github.namiuni.paperplugintemplate.common.component.ComponentStore;
import io.github.namiuni.paperplugintemplate.common.component.ComponentTypes;
import io.github.namiuni.paperplugintemplate.common.user.UserInternal;
import io.github.namiuni.paperplugintemplate.minecraft.paper.component.PaperPlayerComponent;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.ForwardingAudience;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class PaperUser implements UserInternal, ForwardingAudience.Single {

    private final UUID uuid;
    private final ComponentStore store;

    public PaperUser(final UUID uuid, final ComponentStore store) {
        this.uuid = Objects.requireNonNull(uuid, "uuid");
        this.store = Objects.requireNonNull(store, "store");
    }

    @Override
    public PaperPlayerComponent playerComponent() {
        return (PaperPlayerComponent) this.store.getOrThrow(this.uuid, ComponentTypes.PLAYER);
    }

    @Override
    public UUID uuid() {
        return this.uuid;
    }

    @Override
    public String name() {
        return this.get(Identity.NAME).orElseThrow();
    }

    @Override
    public Component displayName() {
        return this.get(Identity.DISPLAY_NAME).orElseThrow();
    }

    @Override
    public Locale locale() {
        return this.get(Identity.LOCALE).orElseThrow();
    }

    @Override
    public Instant lastSeen() {
        return this.playerComponent().lastSeen();
    }

    @Override
    public boolean isOnline() {
        return this.playerComponent().isOnline();
    }

    @Override
    public Audience audience() {
        return this.playerComponent().audience();
    }

    @Override
    public Identity identity() {
        return this.playerComponent().player().identity();
    }

    @Override
    public String toString() {
        return "PaperUser{uuid=%s, store=%s}".formatted(this.uuid, this.store);
    }

    @Override
    public boolean equals(final @Nullable Object that) {
        if (that == null || this.getClass() != that.getClass()) {
            return false;
        }
        final PaperUser other = (PaperUser) that;
        return Objects.equals(this.uuid, other.uuid) && Objects.equals(this.store, other.store);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.uuid, this.store);
    }
}

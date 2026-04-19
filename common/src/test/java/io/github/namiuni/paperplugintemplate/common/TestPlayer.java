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
package io.github.namiuni.paperplugintemplate.common;

import java.util.Optional;
import java.util.UUID;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.ForwardingAudience;
import net.kyori.adventure.identity.Identified;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.pointer.Pointer;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public record TestPlayer(UUID uuid, String name) implements ForwardingAudience.Single, Identified {

    @Override
    public Audience audience() {
        return Audience.empty();
    }

    @Override
    public Identity identity() {
        return Identity.identity(this.uuid);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <V> Optional<V> get(final Pointer<V> pointer) {
        if (pointer == Identity.UUID) {
            return Optional.of((V) this.uuid);
        }
        if (pointer == Identity.NAME) {
            return Optional.of((V) this.name);
        }
        return Optional.empty();
    }

    @Override
    public <V> @Nullable V getOrDefault(final Pointer<V> pointer, final @Nullable V defaultValue) {
        return this.get(pointer).orElse(defaultValue);
    }
}

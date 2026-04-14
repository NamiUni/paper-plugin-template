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
package io.github.namiuni.paperplugintemplate.minecraft.paper.component;

import io.github.namiuni.paperplugintemplate.common.component.components.PlayerComponent;
import java.time.Instant;
import net.kyori.adventure.audience.Audience;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

@NullMarked
public record PaperPlayerComponent(Player player) implements PlayerComponent {

    /// {@inheritDoc}
    @Override
    public Audience audience() {
        return this.player;
    }

    @Override
    public boolean isOnline() {
        return this.player.isOnline();
    }

    @Override
    public Instant lastSeen() {
        return Instant.ofEpochMilli(this.player.getLastSeen());
    }
}

/*
 * PaperPluginTemplate
 *
 * Copyright (c) 2026. Namiu (うにたろう)
 *                     Contributors
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
package io.github.namiuni.paperplugintemplate.minecraft.paper.command;

import io.github.namiuni.paperplugintemplate.api.user.PluginTemplateUserService;
import io.github.namiuni.paperplugintemplate.common.command.CommandSource;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.audience.Audience;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

// TODO: Javadoc
@NullMarked
public final class PaperCommandSource implements CommandSource {

    private final CommandSourceStack source;
    private final PluginTemplateUserService userService;

    // TODO: Javadoc
    public PaperCommandSource(
            final CommandSourceStack source,
            final PluginTemplateUserService userService
    ) {
        this.source = source;
        this.userService = userService;
    }

    // TODO: Javadoc
    public CommandSourceStack paperSource() {
        return this.source;
    }

    /// {@inheritDoc}
    @Override
    public Audience sender() {
        if (this.source.getSender() instanceof Player player) {
            return this.userService.getUser(player.getUniqueId()).orElseThrow();
        }

        return this.source.getSender();
    }

    /// {@inheritDoc}
    @Override
    public @Nullable Audience executor() {
        if (this.source.getExecutor() instanceof Player player) {
            return this.userService.getUser(player.getUniqueId()).orElseThrow();
        }

        return this.source.getExecutor();
    }
}

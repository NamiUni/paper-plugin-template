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
package io.github.namiuni.paperplugintemplate.minecraft.sponge.command;

import io.github.namiuni.paperplugintemplate.api.user.PluginTemplateUserService;
import io.github.namiuni.paperplugintemplate.common.command.CommandSource;
import net.kyori.adventure.audience.Audience;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;

@NullMarked
public final class SpongeCommandSource implements CommandSource {

    private final CommandCause cause;
    private final PluginTemplateUserService userService;

    public SpongeCommandSource(
            final CommandCause cause,
            final PluginTemplateUserService userService
    ) {
        this.cause = cause;
        this.userService = userService;
    }

    public CommandCause spongeCause() {
        return this.cause;
    }

    @Override
    public Audience sender() {
        if (this.cause.subject() instanceof final ServerPlayer player) {
            return this.userService.getUser(player.uniqueId()).orElseThrow();
        }
        return this.cause.audience();
    }

    @Override
    public @Nullable Audience executor() {
        if (this.cause.subject() instanceof final ServerPlayer player) {
            return this.userService.getUser(player.uniqueId()).orElse(null);
        }
        return null;
    }
}

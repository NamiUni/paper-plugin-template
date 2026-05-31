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
package io.github.namiuni.paperplugintemplate.minecraft.paper.command;

import io.github.namiuni.paperplugintemplate.common.command.CommandSource;
import io.github.namiuni.paperplugintemplate.common.command.Commander;
import io.github.namiuni.paperplugintemplate.common.command.SimpleCommander;
import io.github.namiuni.paperplugintemplate.common.command.UserCommander;
import io.github.namiuni.paperplugintemplate.common.user.UserService;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import java.util.Optional;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.identity.Identity;
import org.jspecify.annotations.NullMarked;

@NullMarked
public final class PaperCommandSource implements CommandSource {

    private final CommandSourceStack source;
    private final UserService userService;

    public PaperCommandSource(
            final CommandSourceStack source,
            final UserService userService
    ) {
        this.source = source;
        this.userService = userService;
    }

    public CommandSourceStack paperSource() {
        return this.source;
    }

    @Override
    public Commander sender() {
        final var sender = this.source.getSender();
        final var uuid = sender.get(Identity.UUID);
        if (uuid.isPresent()) {
            final var user = this.userService.getUser(uuid.get()).orElseThrow();
            return new UserCommander(sender, user);
        }

        return new SimpleCommander(sender);
    }

    @Override
    public Optional<Commander> executor() {
        final Audience executor = this.source.getExecutor();
        if (executor == null) {
            return Optional.empty();
        }

        final var uuid = executor.get(Identity.UUID);
        if (uuid.isPresent()) {
            final var user = this.userService.getUser(uuid.get()).orElseThrow();
            return Optional.of(new UserCommander(executor, user));
        }

        return Optional.of(new SimpleCommander(executor));
    }
}

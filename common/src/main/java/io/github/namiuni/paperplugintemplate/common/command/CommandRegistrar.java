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
package io.github.namiuni.paperplugintemplate.common.command;

import io.github.namiuni.paperplugintemplate.common.command.commands.CommandFactory;
import jakarta.inject.Inject;
import java.util.Set;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.incendo.cloud.CommandManager;
import org.jspecify.annotations.NullMarked;

@NullMarked
public final class CommandRegistrar {

    private final CommandManager<CommandSource> commandManager;
    private final Set<CommandFactory> commandFactories;
    private final ComponentLogger logger;

    @Inject
    CommandRegistrar(
            final CommandManager<CommandSource> commandManager,
            final Set<CommandFactory> commandFactories,
            final ComponentLogger logger
    ) {
        this.commandManager = commandManager;
        this.commandFactories = commandFactories;
        this.logger = logger;
    }

    public void registerCommands() {
        this.commandFactories.forEach(factory -> {
            final var command = factory.createCommand();
            this.commandManager.command(command);
            this.logger.debug(
                    "[{}] Registered command: /{} ({})",
                    CommandRegistrar.class.getSimpleName(),
                    command.rootComponent().name(),
                    factory.getClass().getSimpleName()
            );
        });
        this.logger.info("Registered {} command(s).", this.commandFactories.size());
    }
}

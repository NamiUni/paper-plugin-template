/*
 * PluginTemplate
 *
 * Copyright (c) 2025. Namiu/Unitarou
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
package com.github.namiuni.plugintemplate.command;

import com.github.namiuni.plugintemplate.command.commands.PluginCommand;
import com.github.namiuni.plugintemplate.command.commands.RootCommand;
import com.google.inject.Inject;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.jspecify.annotations.NullMarked;

import java.util.Set;

@NullMarked
@SuppressWarnings("UnstableApiUsage")
public final class CommandManager {

    private final LifecycleEventManager<BootstrapContext> lifecycleEventManager;
    private final Set<PluginCommand> commands;

    @Inject
    private CommandManager(
            final LifecycleEventManager<BootstrapContext> lifecycleEventManager,
            final Set<PluginCommand> commands
    ) {
        this.lifecycleEventManager = lifecycleEventManager;
        this.commands = commands;
    }

    public void register() {
        this.lifecycleEventManager.registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            final var rootCommand = new RootCommand();
            final var command = this.commands.stream()
                    .map(PluginCommand::node)
                    .collect(rootCommand::node, LiteralArgumentBuilder::then, LiteralArgumentBuilder::then);

            event.registrar().register(command.build(), rootCommand.description(), rootCommand.aliases());
        });
    }
}

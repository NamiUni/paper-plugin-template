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

/// Iterates the bound [CommandFactory] set and registers each command with
/// the [org.incendo.cloud.CommandManager] at plugin startup.
///
/// Acts as the single registration point for all Cloud commands, decoupling
/// command authoring from command registration. New commands are contributed
/// by adding bindings to the [com.google.inject.multibindings.Multibinder]
/// for [CommandFactory] in the platform Guice module; this class requires no
/// modification as the command set grows.
///
/// ## Thread safety
///
/// [#registerCommands()] is designed to be called exactly once on the
/// bootstrap thread during
/// [io.github.namiuni.paperplugintemplate.common.PluginInitializer#initialize()].
/// Concurrent invocation is not supported and would produce duplicate
/// command registrations.
@NullMarked
public final class CommandRegistrar {

    private final CommandManager<CommandSource> commandManager;
    private final Set<CommandFactory> commandFactories;
    private final ComponentLogger logger;

    /// Constructs a new registrar.
    ///
    /// @param commandManager   the Cloud command manager to which all
    ///                         commands will be registered
    /// @param commandFactories the set of factories contributed via Guice
    ///                         [com.google.inject.multibindings.Multibinder];
    ///                         must not be `null` or contain `null` elements
    /// @param logger           the component-aware logger
    @Inject
    private CommandRegistrar(
            final CommandManager<CommandSource> commandManager,
            final Set<CommandFactory> commandFactories,
            final ComponentLogger logger
    ) {
        this.commandManager = commandManager;
        this.commandFactories = commandFactories;
        this.logger = logger;
    }

    /// Registers all commands contributed by the bound [CommandFactory] set.
    ///
    /// Iterates this registrar's factory set, invokes [CommandFactory#command()]
    /// on each factory, and passes the result to
    /// [org.incendo.cloud.CommandManager#command]. Must be called exactly
    /// once on the bootstrap thread before the server accepts player
    /// connections.
    public void registerCommands() {
        this.commandFactories.forEach(factory -> {
            final var command = factory.command();
            this.commandManager.command(command);
            this.logger.debug(
                    "Registered command: /{} ({})",
                    command.rootComponent().name(),
                    factory.getClass().getSimpleName()
            );
        });
        this.logger.info("Registered {} command(s).", this.commandFactories.size());
    }
}

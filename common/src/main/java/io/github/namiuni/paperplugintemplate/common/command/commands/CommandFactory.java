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
package io.github.namiuni.paperplugintemplate.common.command.commands;

import io.github.namiuni.paperplugintemplate.common.command.CommandSource;
import org.incendo.cloud.Command;
import org.jspecify.annotations.NullMarked;

/// Factory contract for contributing a single [Command] to the plugin's command tree.
///
/// Each implementation encapsulates the construction of one logical command — including
/// its permission node, argument specification, description, and handler — and is
/// contributed to the Guice [com.google.inject.multibindings.Multibinder] for
/// `CommandFactory` in the platform module.
/// [io.github.namiuni.paperplugintemplate.common.command.CommandRegistrar] then iterates
/// the bound set at startup and delegates each call to [#command()], decoupling authorship
/// from registration.
///
/// ## Extending the command set
///
/// To add a new command, implement this interface, annotate the class with `@Singleton`,
/// and add a binding to the `Multibinder`:
///
/// ```java
/// commands.addBinding().to(MyNewCommand.class).in(Scopes.SINGLETON);
/// ```
///
/// No changes to [io.github.namiuni.paperplugintemplate.common.command.CommandRegistrar]
/// or the platform module are required.
///
/// ## Thread safety
///
/// [#command()] is invoked exactly once on the bootstrap thread during
/// [io.github.namiuni.paperplugintemplate.minecraft.paper.PaperBootstrap#bootstrap].
/// Implementations need not be thread-safe with respect to [#command()] itself, but the
/// [Command] returned must be safe for Cloud's execution coordinator to invoke concurrently
/// across multiple sender threads.
@NullMarked
@FunctionalInterface
public interface CommandFactory {

    /// Builds and returns the [Command] contributed by this factory.
    ///
    /// Called exactly once by
    /// [io.github.namiuni.paperplugintemplate.common.command.CommandRegistrar#registerCommands()]
    /// on the bootstrap thread. The returned command is passed directly to
    /// [org.incendo.cloud.CommandManager#command].
    ///
    /// @return the fully constructed command, never `null`
    Command<CommandSource> command();
}

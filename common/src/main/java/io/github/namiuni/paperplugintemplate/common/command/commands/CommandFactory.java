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
package io.github.namiuni.paperplugintemplate.common.command.commands;

import io.github.namiuni.paperplugintemplate.common.command.CommandSource;
import org.incendo.cloud.Command;
import org.jspecify.annotations.NullMarked;

// TODO: Javadoc
@NullMarked
@FunctionalInterface
public interface CommandFactory {

    /// Builds and returns the [org.incendo.cloud.Command] contributed by
    /// this factory.
    ///
    /// Called exactly once by [io.github.namiuni.paperplugintemplate.common.command.CommandRegistrar#registerCommands()]. The
    /// returned command is passed directly to
    /// [org.incendo.cloud.CommandManager#command].
    ///
    /// @return the fully constructed command, never `null`
    Command<CommandSource> command();
}

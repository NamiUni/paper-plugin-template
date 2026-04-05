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

/// Concrete Incendo Cloud command implementations and their shared factory
/// contract.
///
/// This package contains the [io.github.namiuni.paperplugintemplate.common.command.commands.CommandFactory] interface and all built-in
/// command classes. Each class encapsulates one logical command — its
/// permission node, argument specification, description, and handler — and
/// is registered with the Cloud
/// [org.incendo.cloud.CommandManager] at startup by
/// [io.github.namiuni.paperplugintemplate.common.command.CommandRegistrar].
///
/// ## Built-in commands
///
/// | Class | Command | Permission |
/// |---|---|---|
/// | [HelpCommand]   | `/template help [query]` | `template.command.help`   |
/// | [ReloadCommand] | `/template reload`       | `template.command.reload` |
///
/// ## Adding a new command
///
/// Implement `CommandFactory`, annotate the class with `@Singleton`, and
/// add a binding to the Guice `Multibinder` for `CommandFactory` in the
/// platform module. [io.github.namiuni.paperplugintemplate.common.command.CommandRegistrar]
/// picks it up automatically at the next server start; no other changes are
/// required.
///
/// ## Thread safety
///
/// [io.github.namiuni.paperplugintemplate.common.command.commands.CommandFactory#command()] is called once on the bootstrap thread. The
/// [org.incendo.cloud.Command] objects returned must be safe for concurrent
/// invocations across multiple sender threads, as Cloud executes handlers
/// on its async execution coordinator.
package io.github.namiuni.paperplugintemplate.common.command.commands;

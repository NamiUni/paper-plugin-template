/*
 * paper-plugin-template
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
package io.github.namiuni.paperplugintemplate.commands;

import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import java.util.List;
import org.jspecify.annotations.NullMarked;

/// Factory interface for creating Brigadier command trees.
///
/// Implementations are bound via Guice's [com.google.inject.multibindings.Multibinder]
/// and registered with the Paper command system during [io.github.namiuni.paperplugintemplate.TemplateBootstrap#bootstrap].
///
/// Each implementation should produce a single [LiteralCommandNode] that
/// represents the root of a command tree. Sub-commands are added as children of
/// that root node.
@NullMarked
public interface CommandFactory {

    /// Return value indicating that a command execution has failed.
    ///
    /// Use this instead of [com.mojang.brigadier.Command#SINGLE_SUCCESS]
    /// when the command could not complete its intended action.
    int SINGLE_FAILURE = 2;

    /// Builds and returns the root [LiteralCommandNode] for this command.
    ///
    /// @return the constructed command node, never `null`
    LiteralCommandNode<CommandSourceStack> command();

    /// Returns a human-readable description of this command shown in help output.
    ///
    /// @return the command description; defaults to an empty string
    default String description() {
        return ""; // TODO
    }

    /// Returns the list of aliases under which this command can also be invoked.
    ///
    /// @return an immutable list of alias strings; defaults to an empty list
    default List<String> aliases() {
        return List.of();
    }
}

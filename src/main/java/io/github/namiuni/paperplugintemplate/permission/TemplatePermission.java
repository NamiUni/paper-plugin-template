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
package io.github.namiuni.paperplugintemplate.permission;

import org.jspecify.annotations.NullMarked;

/// Enumeration of all permission nodes used by the template plugin.
///
/// Each constant wraps the dot-separated permission string that must be granted
/// to a player (or inherited via a permission plugin) before the associated feature
/// becomes accessible. Use [#node()] to retrieve the raw string for calls such
/// as [org.bukkit.command.CommandSender#hasPermission(String)].
@NullMarked
public enum TemplatePermission {

    /// Grants access to the `/template reload` administration command.
    COMMAND_RELOAD("template.command.admin.reload"); // TODO change the prefix

    private final String node;

    TemplatePermission(final String node) {
        this.node = node;
    }

    /// Returns the raw permission node string.
    ///
    /// @return the dot-separated permission node, never `null`
    public String node() {
        return this.node;
    }
}

/*
 * PaperPluginTemplate
 *
 * Copyright (c) 2026. Namiu (ãã«ããã)
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
package io.github.namiuni.paperplugintemplate.api;

import io.github.namiuni.paperplugintemplate.api.user.PluginTemplateUserService;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NullMarked;

/// Top-level entry point to the public API surface of the template plugin.
///
/// Obtain the singleton instance via [PluginTemplateProvider#pluginTemplate()]
/// after the plugin has finished bootstrapping. All services reachable through
/// this interface are safe to use from any thread unless their own documentation
/// states otherwise.
///
/// ## Typical usage
///
/// ```java
/// PluginTemplate api = PluginTemplateProvider.pluginTemplate();
/// api.userService().getUser(player).ifPresent(user -> ...);
/// ```
///
/// @see PluginTemplateProvider
@NullMarked
@ApiStatus.NonExtendable
public interface PluginTemplate {

    /// Returns the service for accessing and managing plugin-tracked players.
    ///
    /// The returned service is a singleton; repeated calls always return the
    /// same instance.
    ///
    /// @return the user service, never `null`
    PluginTemplateUserService userService();
}

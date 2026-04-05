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

/// Public API entry point for third-party plugins integrating with the template plugin.
///
/// This package contains two types:
///
/// - [io.github.namiuni.paperplugintemplate.api.PluginTemplate] — the top-level facade that provides access to all
///   plugin services. Obtain the singleton via [io.github.namiuni.paperplugintemplate.api.PluginTemplateProvider#pluginTemplate()].
/// - [io.github.namiuni.paperplugintemplate.api.PluginTemplateProvider] — the static accessor for the `PluginTemplate`
///   singleton.
///
/// ## Usage
///
/// ```java
/// PluginTemplate api = PluginTemplateProvider.pluginTemplate();
/// api.userService().getUser(uuid).ifPresent(user -> ...);
/// ```
///
/// ## Lifecycle
///
/// [io.github.namiuni.paperplugintemplate.api.PluginTemplateProvider#pluginTemplate()] throws [java.lang.NullPointerException]
/// if called before this plugin has finished enabling. Always access the API from an `onEnable` callback or later.
///
/// ## Stability
///
/// Only types in this package and its sub-packages form the stable public API.
/// Classes in other packages are internal and may change without notice.
///
/// ## Thread safety
///
/// All types in this package are safe to call from any thread once the plugin has finished enabling.
package io.github.namiuni.paperplugintemplate.api;

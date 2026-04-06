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
package io.github.namiuni.paperplugintemplate.api;

import java.util.Objects;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/// Static registry that holds the singleton [PluginTemplate] instance.
///
/// Third-party plugins obtain the public API reference through
/// [#pluginTemplate()]. The instance is written exactly once during plugin
/// bootstrap — before the server accepts player connections — and is never
/// replaced thereafter.
///
/// ## Thread safety
///
/// The backing field is written once at startup and never modified again.
/// All subsequent reads via [#pluginTemplate()] are therefore safe from any
/// thread without additional synchronization.
@NullMarked
public final class PluginTemplateProvider {

    private static @Nullable PluginTemplate instance;

    private PluginTemplateProvider() {
    }

    /// Registers the singleton [PluginTemplate] instance.
    ///
    /// Called exactly once by the plugin internals during bootstrap. Invoking
    /// this method a second time would silently overwrite the registered
    /// instance and is therefore prohibited outside the plugin's own
    /// initialization path.
    ///
    /// @param instance the fully initialized plugin API instance; must not be `null`
    @ApiStatus.Internal
    public static void register(final PluginTemplate instance) {
        PluginTemplateProvider.instance = instance;
    }

    /// Returns the singleton [PluginTemplate] instance.
    ///
    /// @return the registered plugin API, never `null`
    /// @throws NullPointerException if called before the plugin has finished
    ///         bootstrapping and [#register] has not yet been invoked
    public static PluginTemplate pluginTemplate() { // TODO: change the method name
        return Objects.requireNonNull(PluginTemplateProvider.instance, "PluginTemplate not initialized!"); // TODO: change the plugin name
    }
}

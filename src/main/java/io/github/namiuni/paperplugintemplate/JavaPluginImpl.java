/*
 * paper-plugin-template
 *
 * Copyright (c) 2026. Namiu/Unitarou
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
package io.github.namiuni.paperplugintemplate;

import jakarta.inject.Inject;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.NullMarked;

/// Main plugin class for the template plugin.
///
/// This class is instantiated by the Guice injector via [PluginBootstrapImpl]
/// rather than by the Paper plugin framework directly. The `@Inject` constructor
/// enforces that all dependencies are satisfied before the plugin becomes active.
@NullMarked
public final class JavaPluginImpl extends JavaPlugin {

    /// Constructs a new `JavaPluginImpl` instance.
    ///
    /// This constructor is package-private and intended for use by the Guice injector only.
    @Inject
    private JavaPluginImpl() {
    }

    /// {@inheritDoc}
    @Override
    public void onEnable() {
        super.onEnable();
    }

    /// {@inheritDoc}
    @Override
    public void onDisable() {
        super.onDisable();
    }
}

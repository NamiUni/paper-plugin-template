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
package io.github.namiuni.paperplugintemplate.configuration;

import io.github.namiuni.paperplugintemplate.configuration.annotations.ConfigHeader;
import io.github.namiuni.paperplugintemplate.configuration.annotations.ConfigName;
import org.jspecify.annotations.NullMarked;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

/// Root configuration record for the template plugin.
///
/// This record is automatically serialized to and deserialized from
/// `config.conf` in the plugin data directory by
/// [ConfigurationLoader]. Add configuration fields as record components
/// and Configurate will handle the file mapping.
///
/// Use [#DEFAULT] as the fallback value when the file does not yet exist
/// or when a field is missing from an existing file.
@NullMarked
@ConfigSerializable
@ConfigName("config.conf")
@ConfigHeader("""
        Main configuration.
        """)
public record PrimaryConfiguration() {

    /// Default instance used as a fallback when no configuration file is present.
    public static final PrimaryConfiguration DEFAULT = new PrimaryConfiguration();
}

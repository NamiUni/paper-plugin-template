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
package io.github.namiuni.paperplugintemplate.common.configuration.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.jspecify.annotations.NullMarked;

/// Specifies the file name used when persisting a configuration record to disk.
///
/// Place this annotation on a configuration record class to declare the filename
/// (relative to the plugin's data directory) that [io.github.namiuni.paperplugintemplate.common.configuration.ConfigurationLoader]
/// will read from and write to.
/// <pre>
/// `record MyConfig(String someValue){}`</pre>
@NullMarked
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ConfigName {

    /**
     * The configuration file name, including extension (e.g. `"config.conf"`).
     *
     * @return the file name relative to the plugin data directory
     */
    String value();
}

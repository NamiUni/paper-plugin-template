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
package io.github.namiuni.paperplugintemplate.common.configuration.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.jspecify.annotations.NullMarked;

/// Specifies the comment header written at the top of a configuration file.
///
/// Place this annotation on a configuration record class to inject a
/// descriptive comment block when the file is first generated or saved by
/// [io.github.namiuni.paperplugintemplate.common.configuration.ConfigurationLoader]. The value is forwarded directly to Configurate's
/// node header option.
///
/// ```java
/// @ConfigHeader("""
///         Main configuration.
///         Restart the server after editing.""")
/// public record MyConfig(String someValue) {}
/// ```
@NullMarked
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ConfigHeader {

    /// The header text to prepend to the configuration file.
    ///
    /// @return the header string; may contain newlines
    String value();
}

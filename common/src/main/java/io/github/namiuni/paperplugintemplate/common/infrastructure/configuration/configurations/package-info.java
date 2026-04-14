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

/// Typed configuration record definitions for the plugin.
///
/// Each record in this package corresponds to one configuration file on disk and
/// is annotated with
/// [io.github.namiuni.paperplugintemplate.common.infrastructure.configuration.annotations.ConfigName]
/// (file name) and
/// [io.github.namiuni.paperplugintemplate.common.infrastructure.configuration.annotations.ConfigHeader]
/// (file header comment).
///
/// ## Available configurations
///
/// | Record | File | Description |
/// |---|---|---|
/// | [io.github.namiuni.paperplugintemplate.common.infrastructure.configuration.configurations.PrimaryConfiguration] | `config.conf` | Storage backend, connection pool, and cache settings |
///
/// ## Adding a new configuration
///
/// 1. Create a `@ConfigSerializable` record annotated with `@ConfigName` and
///    `@ConfigHeader` in this package.
/// 2. Declare a `DEFAULT` constant for use as the fallback instance.
/// 3. Add a `ConfigurationLoader` `@Provides` binding in
///    [io.github.namiuni.paperplugintemplate.common.infrastructure.InfrastructureModule].
/// 4. Bind the holder and `Reloadable` in the same module, mirroring the
///    `PrimaryConfiguration` bindings.
package io.github.namiuni.paperplugintemplate.common.infrastructure.configuration.configurations;

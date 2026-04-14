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

/// Infrastructure layer providing cross-cutting technical concerns for the plugin.
///
/// This package and its sub-packages contain the technical building blocks that support
/// the application layer without expressing any domain logic:
///
/// - **`configuration`** — HOCON file loading, hot-reload via [io.github.namiuni.paperplugintemplate.common.infrastructure.configuration.ConfigurationHolder],
///   and annotation-driven metadata ([io.github.namiuni.paperplugintemplate.common.infrastructure.configuration.annotations.ConfigName],
///   [io.github.namiuni.paperplugintemplate.common.infrastructure.configuration.annotations.ConfigHeader]).
/// - **`persistence`** — storage backend abstraction via [io.github.namiuni.paperplugintemplate.common.infrastructure.persistence.UserRepository],
///   with SQL (JDBI + HikariCP + Flyway) and JSON flat-file implementations.
/// - **`translation`** — Adventure [net.kyori.adventure.translation.Translator] loading,
///   hot-reload, and operator-editable `.properties` file management.
/// - [io.github.namiuni.paperplugintemplate.common.infrastructure.Reloadable] — functional
///   contract for components that support hot-reload without a server restart.
/// - [io.github.namiuni.paperplugintemplate.common.infrastructure.DataDirectory] — Guice
///   binding annotation that qualifies a [java.nio.file.Path] as the plugin data directory.
///
/// ## Thread safety
///
/// Infrastructure components in this package are designed for use from multiple threads.
/// See each type's documentation for its specific thread-safety contract.
package io.github.namiuni.paperplugintemplate.common.infrastructure;

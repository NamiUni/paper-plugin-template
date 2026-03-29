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
package io.github.namiuni.paperplugintemplate.user.storage;

import org.jspecify.annotations.NullMarked;

/// Supported storage backend types.
///
/// The active type is determined at startup from
/// [io.github.namiuni.paperplugintemplate.configuration.PrimaryConfiguration.StorageConfig]
/// and cannot be changed without restarting the server.
@NullMarked
public enum StorageType {

    /// Embedded H2 SQL database. Stores data in a file inside the plugin data directory.
    /// No external server is required; suitable for single-server deployments.
    H2,

    /// External MySQL (or MariaDB) database server.
    /// Requires a running server and valid credentials in the configuration.
    MYSQL,

    /// Flat JSON file storage. Each user is persisted as a separate `.json` file.
    /// Human-readable and easy to inspect, but not suitable for high-concurrency workloads.
    JSON,
}

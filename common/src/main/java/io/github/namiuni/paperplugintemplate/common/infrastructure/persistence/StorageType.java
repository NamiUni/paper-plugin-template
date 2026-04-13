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
package io.github.namiuni.paperplugintemplate.common.infrastructure.persistence;

import org.jspecify.annotations.NullMarked;

/// Supported storage backend types.
///
/// The active type is determined at startup from the primary configuration
/// and cannot be changed without restarting the server.
@NullMarked
public enum StorageType {

    /// Embedded H2 SQL database. No external server required; suitable for
    /// single-server deployments. Uses the same migration scripts and UUID
    /// binding strategy as [#MYSQL] (`MODE=MySQL`).
    H2,

    /// External MySQL (or MariaDB) database server. UUIDs are persisted as
    /// `BINARY(16)` for compact storage.
    MYSQL,

    /// External PostgreSQL database server. UUIDs are persisted using
    /// PostgreSQL's native `UUID` type.
    POSTGRESQL,

    /// Flat JSON file storage. Human-readable but not suitable for
    /// high-concurrency workloads.
    JSON,
}

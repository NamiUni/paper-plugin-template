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

/// Storage abstraction and backend implementations for player profile persistence.
///
/// This package defines the persistence contracts and shared value types:
///
/// - [io.github.namiuni.paperplugintemplate.common.infrastructure.persistence.UserRepository] —
///   storage-agnostic async data-access contract for [io.github.namiuni.paperplugintemplate.common.infrastructure.persistence.UserRecord].
/// - [io.github.namiuni.paperplugintemplate.common.infrastructure.persistence.UserRecord] —
///   immutable persistent snapshot of a tracked player's profile.
/// - [io.github.namiuni.paperplugintemplate.common.infrastructure.persistence.StorageType] —
///   enum of supported backend types (`H2`, `MYSQL`, `POSTGRESQL`, `JSON`).
/// - [io.github.namiuni.paperplugintemplate.common.infrastructure.persistence.StorageDialect] —
///   sealed interface that encapsulates SQL dialect differences (UUID binding strategy,
///   Flyway migration location, row mapping).
/// - [io.github.namiuni.paperplugintemplate.common.infrastructure.persistence.FlywayLogger] —
///   [org.flywaydb.core.api.logging.LogCreator] that routes Flyway output through the
///   Adventure component logger.
///
/// ## Sub-packages
///
/// - `sql` — JDBI 3 + HikariCP implementation supporting H2, MySQL, and PostgreSQL.
/// - `json` — flat-file JSON implementation for single-server, low-traffic deployments.
///
/// @apiNote Callers should always access storage through the user service rather than
///          directly through [io.github.namiuni.paperplugintemplate.common.infrastructure.persistence.UserRepository].
///          Direct use bypasses the in-memory cache and may cause redundant I/O.
package io.github.namiuni.paperplugintemplate.common.infrastructure.persistence;

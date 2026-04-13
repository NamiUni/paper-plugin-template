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
package io.github.namiuni.paperplugintemplate.common.infrastructure.persistence.sql;

import io.github.namiuni.paperplugintemplate.common.infrastructure.persistence.UserComponent;
import java.util.Optional;
import java.util.UUID;
import org.jdbi.v3.sqlobject.SqlObject;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindMethods;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.transaction.Transaction;
import org.jspecify.annotations.NullMarked;

/// JDBI SQL Object providing low-level access to the `users` table.
///
/// All statements use portable SQL that runs against H2 (`MODE=MySQL`),
/// MySQL/MariaDB, and PostgreSQL without modification.
/// Vendor-specific upsert syntax is deliberately avoided; the portable
/// update-then-insert strategy is implemented entirely in [#upsert].
///
/// ## Schema management
///
/// The `users` table is created and versioned exclusively by Flyway
/// (see `classpath:db/migration/<vendor>`). This DAO contains no DDL;
/// schema changes must be expressed as new numbered migration scripts.
///
/// ## Parameter binding
///
/// Record components are bound via `@BindMethods`, which invokes each
/// no-arg accessor and maps the method name to the SQL parameter name.
/// [UserComponent#uuid()] is bound as `:uuid` using the dialect-specific
/// [org.jdbi.v3.core.argument.QualifiedArgumentFactory] registered on the
/// [org.jdbi.v3.core.Jdbi] instance by `StorageModule`:
///
/// - MySQL / H2: serializes [java.util.UUID] to a 16-byte `BINARY(16)` value.
/// - PostgreSQL: passes the [java.util.UUID] directly via
///   [java.sql.PreparedStatement#setObject], relying on the PostgreSQL JDBC
///   driver's native `uuid` type support.
///
/// ## Upsert strategy
///
/// [#upsert] implements a portable update-then-insert pattern. It attempts
/// `UPDATE` first; if zero rows were affected the player is new, so `INSERT`
/// is performed. A narrow TOCTOU race where two concurrent callers both
/// observe zero rows from `UPDATE` is resolved by catching the resulting
/// duplicate-key exception on `INSERT` and retrying `UPDATE`.
///
/// ## Thread safety
///
/// JDBI acquires a fresh `Handle` (JDBC connection) for each SQL Object
/// invocation. This interface carries no mutable state; all thread-safety
/// concerns are delegated to the underlying
/// [com.zaxxer.hikari.HikariDataSource] connection pool and JDBC driver.
@NullMarked
public interface UserDao extends SqlObject {

    /// Returns the profile for `uuid`, or [Optional#empty()] if no row
    /// exists.
    ///
    /// The result is mapped by the [org.jdbi.v3.core.mapper.RowMapper]
    /// registered on the [org.jdbi.v3.core.Jdbi] instance rather than by a
    /// method-level `@UseRowMapper` annotation, so the correct dialect
    /// strategy is applied automatically.
    ///
    /// @param uuid the player UUID to look up
    /// @return the mapped [UserComponent] wrapped in [Optional], or [Optional#empty()] if absent
    @SqlQuery("SELECT uuid, name, last_seen FROM users WHERE uuid = :uuid")
    Optional<UserComponent> findByUuid(@Bind("uuid") UUID uuid);

    /// Inserts a new user row.
    ///
    /// Prefer [#upsert] over calling this method directly; direct calls do
    /// not handle the case where the row already exists.
    ///
    /// @param userComponent the record to insert; components bound via `@BindMethods`
    @SqlUpdate("INSERT INTO users (uuid, name, last_seen) VALUES (:uuid, :name, :lastSeen)")
    void insert(@BindMethods UserComponent userComponent);

    /// Updates the `name` and `last_seen` columns for an existing row.
    ///
    /// Prefer [#upsert] over calling this method directly.
    ///
    /// @param userComponent the record to update; components bound via `@BindMethods`
    /// @return the number of affected rows; `0` indicates no row existed for this UUID
    @SqlUpdate("UPDATE users SET name = :name, last_seen = :lastSeen WHERE uuid = :uuid")
    int update(@BindMethods UserComponent userComponent);

    /// Removes the row for `uuid`, if it exists.
    ///
    /// This operation is a no-op if no row exists for the given UUID.
    ///
    /// @param uuid the player UUID to delete
    @SqlUpdate("DELETE FROM users WHERE uuid = :uuid")
    void deleteByUuid(@Bind("uuid") UUID uuid);

    /// Inserts or updates a user in a portable, database-agnostic
    /// transaction.
    ///
    /// Attempts [#update] first; if zero rows were affected the player is
    /// new, so [#insert] is performed. A narrow TOCTOU race where two
    /// concurrent callers both observe zero rows from `update` is resolved
    /// by catching the resulting duplicate-key exception on `insert` and
    /// retrying `update`.
    ///
    /// @param userComponent the user data to persist
    /// @implNote This `default` method calls sibling SQL methods on the
    ///           same transactional handle via the JDBI proxy. The
    ///           `@Transaction` annotation wraps the entire method in a
    ///           single database transaction.
    @Transaction
    default void upsert(final UserComponent userComponent) {
        if (this.update(userComponent) == 0) {
            try {
                this.insert(userComponent);
            } catch (final Exception _) {
                // Concurrent insert raced us; the row now exists — retry update.
                this.update(userComponent);
            }
        }
    }
}

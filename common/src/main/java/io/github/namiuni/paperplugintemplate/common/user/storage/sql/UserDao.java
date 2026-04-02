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
package io.github.namiuni.paperplugintemplate.common.user.storage.sql;

import io.github.namiuni.paperplugintemplate.common.user.storage.UserProfile;
import java.util.Optional;
import java.util.UUID;
import org.jdbi.v3.sqlobject.SqlObject;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindMethods;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.statement.UseRowMapper;
import org.jdbi.v3.sqlobject.transaction.Transaction;
import org.jspecify.annotations.NullMarked;

/// JDBI SQL Object providing low-level access to the `users` table.
///
/// All statements use ANSI-compatible SQL so the same interface works against
/// H2 (`MODE=MySQL`), MySQL, and MariaDB without dialect branching.
/// Vendor-specific upsert syntax such as `ON DUPLICATE KEY UPDATE` or
/// `ON CONFLICT DO UPDATE` is deliberately avoided.
///
/// ## Parameter binding
///
/// Record components are bound via `@BindMethods`, which invokes each no-arg
/// accessor on the record and maps the method name to the SQL parameter. For
/// example, [UserProfile#uuid()] binds `:uuid` and [UserProfile#name()] binds
/// `:name`.
///
/// ## Upsert strategy
///
/// [#upsert] implements a portable update-then-insert pattern. It attempts
/// `UPDATE` first; if no row was affected the player is new, so `INSERT` is
/// performed within the same transaction. A narrow TOCTOU race where two
/// concurrent callers both observe zero rows from `UPDATE` is resolved by
/// catching the resulting duplicate-key exception on `INSERT` and retrying
/// `UPDATE`.
///
/// @see SqlObject for the handle-access mechanism used by [#upsert]
@NullMarked
public interface UserDao extends SqlObject {

    /// Creates the `users` table if it does not already exist.
    ///
    /// Called once during [JdbiUserRepository#initialize()].
    @SqlUpdate("""
            CREATE TABLE IF NOT EXISTS users (
                uuid      VARCHAR(36) NOT NULL,
                name      VARCHAR(16) NOT NULL,
                last_seen BIGINT      NOT NULL DEFAULT 0,
                PRIMARY KEY (uuid)
            )
            """)
    void createTable();

    /// Returns the profile for `uuid`, or [Optional#empty()] if no row exists.
    ///
    /// @param uuid the player UUID to look up
    /// @return the mapped [UserProfile] wrapped in [Optional], or [Optional#empty()] if absent
    @SqlQuery("SELECT uuid, name, last_seen FROM users WHERE uuid = :uuid")
    @UseRowMapper(UserProfileMapper.class)
    Optional<UserProfile> findByUuid(@Bind("uuid") UUID uuid);

    /// Inserts a new user row.
    ///
    /// Prefer [#upsert] over calling this method directly; direct calls do not
    /// handle the case where the row already exists.
    ///
    /// @param userProfile the record to insert; components bound via `@BindMethods`
    @SqlUpdate("INSERT INTO users (uuid, name, last_seen) VALUES (:uuid, :name, :lastSeen)")
    void insert(@BindMethods UserProfile userProfile);

    /// Updates the `name` and `last_seen` columns for an existing row.
    ///
    /// Prefer [#upsert] over calling this method directly.
    ///
    /// @param userProfile the record to update; components bound via `@BindMethods`
    /// @return the number of affected rows; `0` indicates no row existed for this UUID
    @SqlUpdate("UPDATE users SET name = :name, last_seen = :lastSeen WHERE uuid = :uuid")
    int update(@BindMethods UserProfile userProfile);

    /// Removes the row for `uuid`, if it exists.
    ///
    /// This operation is a no-op if no row exists for the given UUID.
    ///
    /// @param uuid the player UUID to delete
    @SqlUpdate("DELETE FROM users WHERE uuid = :uuid")
    void deleteByUuid(@Bind("uuid") UUID uuid);

    /// Inserts or updates a user in a portable, database-agnostic transaction.
    ///
    /// Attempts [#update] first; if zero rows were affected the player is new,
    /// so [#insert] is performed. A narrow TOCTOU race where two concurrent
    /// callers both observe zero rows from `update` is resolved by catching the
    /// resulting duplicate-key exception on `insert` and retrying `update`.
    ///
    /// @param userProfile the user data to persist
    /// @implNote This `default` method calls sibling SQL methods on the same
    ///           transactional handle via the JDBI proxy. The `@Transaction` annotation
    ///           wraps the entire method in a single database transaction.
    @Transaction
    default void upsert(final UserProfile userProfile) {
        if (this.update(userProfile) == 0) {
            try {
                this.insert(userProfile);
            } catch (final Exception _) {
                // Concurrent insert raced us; the row now exists — retry update.
                this.update(userProfile);
            }
        }
    }
}

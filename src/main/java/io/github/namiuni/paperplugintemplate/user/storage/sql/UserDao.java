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
package io.github.namiuni.paperplugintemplate.user.storage.sql;

import io.github.namiuni.paperplugintemplate.user.storage.UserProfile;
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

/// JDBI SQL Object defining low-level data access operations for the `users` table.
///
/// All statements use ANSI-compatible SQL so the same interface works against H2,
/// MySQL, and PostgreSQL without modification. The upsert pattern uses a
/// `default` method that attempts an `UPDATE` first and falls back to `INSERT`,
/// avoiding any database-vendor-specific syntax such as
/// `ON DUPLICATE KEY UPDATE` (MySQL) or `ON CONFLICT DO UPDATE` (PostgreSQL).
///
/// Record components are bound via [@BindMethods], which calls each no-arg accessor
/// on the record and maps the method name to the matching SQL parameter name.
/// For example, [UserProfile#uuid()] binds `:uuid` and [UserProfile#name()] binds `:name`.
///
/// Extends [SqlObject] to allow the `upsert` default method to call sibling
/// statements within the same transactional handle.
@NullMarked
public interface UserDao extends SqlObject {

    /// Creates the `users` table if it does not already exist.
    @SqlUpdate("""
            CREATE TABLE IF NOT EXISTS users (
                uuid VARCHAR(36) NOT NULL,
                name VARCHAR(16) NOT NULL,
                PRIMARY KEY (uuid)
            )
            """)
    void createTable();

    /// Looks up a user by their UUID string.
    ///
    /// @param uuid the uuid
    /// @return an [Optional] containing the mapped [UserProfile], or empty if absent
    @SqlQuery("SELECT uuid, name FROM users WHERE uuid = :uuid")
    @UseRowMapper(UserProfileMapper.class)
    Optional<UserProfile> findByUuid(@Bind("uuid") UUID uuid);

    /// Inserts a new user row.
    ///
    /// Callers should prefer [#upsert(UserProfile)] over calling this method directly.
    ///
    /// @param userProfile the record whose components are bound via [@BindMethods]
    @SqlUpdate("INSERT INTO users (uuid, name) VALUES (:uuid, :name)")
    void insert(@BindMethods UserProfile userProfile);

    /// Updates the `name` column for an existing user.
    ///
    /// Callers should prefer [#upsert(UserProfile)] over calling this method directly.
    ///
    /// @param userProfile the record whose components are bound via [@BindMethods]
    /// @return the number of rows affected; `0` means no row existed for this UUID
    @SqlUpdate("UPDATE users SET name = :name WHERE uuid = :uuid")
    int update(@BindMethods UserProfile userProfile);

    /// Removes the row for the given UUID, if it exists.
    ///
    /// @param uuid the uuid
    @SqlUpdate("DELETE FROM users WHERE uuid = :uuid")
    void deleteByUuid(@Bind("uuid") UUID uuid);

    /// Inserts or updates a user in a single database-portable transaction.
    ///
    /// Attempts an `UPDATE` first. If no row was affected (i.e. the player is
    /// new), an `INSERT` is performed. A narrow race condition where two
    /// concurrent callers both see zero rows from `UPDATE` is handled by catching
    /// the resulting duplicate-key violation on `INSERT` and retrying the `UPDATE`.
    ///
    /// @param userProfile the user data to persist
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

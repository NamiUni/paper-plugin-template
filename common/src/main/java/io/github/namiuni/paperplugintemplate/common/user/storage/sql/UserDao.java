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

/// JDBI SQL Object for low-level access to the `users` table.
///
/// All statements use ANSI-compatible SQL so the same interface works against H2,
/// MySQL, and PostgreSQL. The upsert pattern uses a `default` method that attempts
/// `UPDATE` first and falls back to `INSERT`, avoiding vendor-specific syntax such
/// as `ON DUPLICATE KEY UPDATE` or `ON CONFLICT DO UPDATE`.
///
/// Record components are bound via `@BindMethods`, which calls each no-arg
/// accessor on the record and maps the method name to the SQL parameter.
/// For example, [UserProfile#uuid()] binds `:uuid` and [UserProfile#name()] binds `:name`.
///
/// Extends [SqlObject] so that [#upsert] can call sibling statements on the
/// same transactional handle.
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

    /// Returns the profile for `uuid`.
    ///
    /// @param uuid the player UUID to look up
    /// @return the mapped [UserProfile], or empty if absent
    @SqlQuery("SELECT uuid, name FROM users WHERE uuid = :uuid")
    @UseRowMapper(UserProfileMapper.class)
    Optional<UserProfile> findByUuid(@Bind("uuid") UUID uuid);

    /// Inserts a new user row.
    ///
    /// Prefer [#upsert] over calling this method directly.
    ///
    /// @param userProfile the record to insert; components bound via `@BindMethods`
    @SqlUpdate("INSERT INTO users (uuid, name) VALUES (:uuid, :name)")
    void insert(@BindMethods UserProfile userProfile);

    /// Updates the `name` column for an existing user.
    ///
    /// Prefer [#upsert] over calling this method directly.
    ///
    /// @param userProfile the record to update; components bound via `@BindMethods`
    /// @return the number of affected rows; `0` means no row existed for this UUID
    @SqlUpdate("UPDATE users SET name = :name WHERE uuid = :uuid")
    int update(@BindMethods UserProfile userProfile);

    /// Removes the row for `uuid`, if it exists.
    ///
    /// @param uuid the player UUID to delete
    @SqlUpdate("DELETE FROM users WHERE uuid = :uuid")
    void deleteByUuid(@Bind("uuid") UUID uuid);

    /// Inserts or updates a user in a portable, database-agnostic transaction.
    ///
    /// Attempts `UPDATE` first; if no row was affected the player is new, so
    /// `INSERT` is performed. A narrow race where two concurrent callers both
    /// see zero rows from `UPDATE` is resolved by catching the resulting
    /// duplicate-key violation and retrying the `UPDATE`.
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

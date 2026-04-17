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
package io.github.namiuni.paperplugintemplate.common.infrastructure.storage.sql;

import io.github.namiuni.paperplugintemplate.common.infrastructure.storage.UserRecord;
import java.util.Optional;
import java.util.UUID;
import org.jdbi.v3.sqlobject.SqlObject;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindMethods;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.transaction.Transaction;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface UserDao extends SqlObject {

    @SqlQuery("SELECT uuid, name, last_seen FROM users WHERE uuid = :uuid")
    Optional<UserRecord> findByUuid(@Bind("uuid") UUID uuid);

    @SqlUpdate("INSERT INTO users (uuid, name, last_seen) VALUES (:uuid, :name, :lastSeen)")
    void insert(@BindMethods UserRecord userRecord);

    @SqlUpdate("UPDATE users SET name = :name, last_seen = :lastSeen WHERE uuid = :uuid")
    int update(@BindMethods UserRecord userRecord);

    @SqlUpdate("DELETE FROM users WHERE uuid = :uuid")
    void deleteByUuid(@Bind("uuid") UUID uuid);

    @Transaction
    default void upsert(final UserRecord userRecord) {
        if (this.update(userRecord) == 0) {
            try {
                this.insert(userRecord);
            } catch (final Exception _) {
                // Concurrent insert raced us; the row now exists — retry update.
                this.update(userRecord);
            }
        }
    }
}

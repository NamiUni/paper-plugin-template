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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.UUID;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.jspecify.annotations.NullMarked;

/// JDBI [RowMapper] that converts a SQL result row into a [UserProfile] record.
///
/// Timestamps are stored as epoch-millisecond [BIGINT] values to maintain
/// cross-database compatibility between H2 and MySQL without relying on
/// database-specific `TIMESTAMP` semantics.
@NullMarked
public final class UserProfileMapper implements RowMapper<UserProfile> {

    /// Constructs a new `UserDataMapper`.
    public UserProfileMapper() {
    }

    /// {@inheritDoc}
    @Override
    public UserProfile map(final ResultSet rs, final StatementContext ctx) throws SQLException {
        return new UserProfile(
                UUID.fromString(rs.getString("uuid")), // TODO: replace fast-json
                rs.getString("name"),
                Instant.ofEpochMilli(rs.getLong("last_seen"))
        );
    }
}

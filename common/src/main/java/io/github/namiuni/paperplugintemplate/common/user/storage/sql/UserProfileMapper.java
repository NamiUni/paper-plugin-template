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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.UUID;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.jspecify.annotations.NullMarked;

/// JDBI [RowMapper] that converts a SQL result row into a [UserProfile] record.
///
/// ## Column conventions
///
/// - `uuid`: stored as `VARCHAR(36)` and parsed via `UUID.fromString(String)`.
/// - `last_seen`: stored as an epoch-millisecond `BIGINT` to maintain
///   cross-database compatibility between H2 and MySQL, both of which handle
///   `BIGINT` identically regardless of time-zone settings.
@NullMarked
public final class UserProfileMapper implements RowMapper<UserProfile> {

    /// Constructs a new `UserProfileMapper`.
    public UserProfileMapper() {
    }

    /// Maps the current row of `rs` to a new [UserProfile].
    ///
    /// @param rs  the result set positioned at the current row
    /// @param ctx the statement context
    /// @return a [UserProfile] populated from the current row
    /// @throws SQLException if a column cannot be read from `rs`
    @Override
    public UserProfile map(final ResultSet rs, final StatementContext ctx) throws SQLException {
        return new UserProfile(
                UUID.fromString(rs.getString("uuid")),
                rs.getString("name"),
                Instant.ofEpochMilli(rs.getLong("last_seen"))
        );
    }
}

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

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.jdbi.v3.core.argument.QualifiedArgumentFactory;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jspecify.annotations.NullMarked;

/// Encapsulates SQL dialect differences between MySQL/H2 and PostgreSQL.
///
/// Sealed to make the exhaustive variant set statically known to the
/// compiler; `switch` expressions over this type are exhaustively checked
/// without a `default` arm.
///
/// ## Thread safety
///
/// Both record implementations are stateless. All methods return freshly
/// constructed objects or primitives. Safe to call from any thread.
@NullMarked
public sealed interface StorageDialect permits StorageDialect.MySQL, StorageDialect.PostgreSQL {

    String migrationLocation();

    QualifiedArgumentFactory uuidArgumentFactory();

    RowMapper<UserRecord> profileMapper();

    private static byte[] uuidToBytes(final UUID uuid) {
        final ByteBuffer bb = ByteBuffer.allocate(16);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        return bb.array();
    }

    private static UUID uuidFromBytes(final byte[] bytes) {
        final ByteBuffer bb = ByteBuffer.wrap(bytes);
        return new UUID(bb.getLong(), bb.getLong());
    }

    @NullMarked
    record MySQL() implements StorageDialect {

        private static final String LOCATION = "storage/migration/mysql";

        @Override
        public String migrationLocation() {
            return LOCATION;
        }

        @Override
        public QualifiedArgumentFactory uuidArgumentFactory() {
            return (_, value, _) -> {
                if (!(value instanceof final UUID uuid)) {
                    return Optional.empty();
                }
                final byte[] bytes = uuidToBytes(uuid);
                return Optional.of((pos, stmt, _) -> stmt.setBytes(pos, bytes));
            };
        }

        @Override
        public RowMapper<UserRecord> profileMapper() {
            return (rs, _) -> new UserRecord(
                    uuidFromBytes(rs.getBytes("uuid")),
                    rs.getString("name"),
                    Instant.ofEpochMilli(rs.getLong("last_seen"))
            );
        }
    }

    @NullMarked
    record PostgreSQL() implements StorageDialect {

        private static final String LOCATION = "storage/migration/postgresql";

        @Override
        public String migrationLocation() {
            return LOCATION;
        }

        @Override
        public QualifiedArgumentFactory uuidArgumentFactory() {
            return (_, value, _) -> {
                if (!(value instanceof UUID)) {
                    return Optional.empty();
                }
                return Optional.of((pos, stmt, _) -> stmt.setObject(pos, value));
            };
        }

        @Override
        public RowMapper<UserRecord> profileMapper() {
            return (rs, _) -> new UserRecord(
                    rs.getObject("uuid", UUID.class),
                    rs.getString("name"),
                    Instant.ofEpochMilli(rs.getLong("last_seen"))
            );
        }
    }
}

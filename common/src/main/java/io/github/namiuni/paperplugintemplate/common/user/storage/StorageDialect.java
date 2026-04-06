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
package io.github.namiuni.paperplugintemplate.common.user.storage;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.jdbi.v3.core.argument.QualifiedArgumentFactory;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jspecify.annotations.NullMarked;

/// Encapsulates SQL dialect differences that cannot be bridged by a single
/// migration script or a single JDBI argument / mapper strategy.
///
/// ## Why a sealed interface?
///
/// MySQL/H2 and PostgreSQL differ on two axes that are tightly coupled:
///
/// - **UUID storage**: MySQL stores UUIDs as `BINARY(16)` (compact, ordered
///   bytes); PostgreSQL stores them as the native `UUID` type (string-like
///   on the wire, natively indexed).
/// - **Flyway migration location**: each vendor has its own DDL under
///   `storage/migration/<vendor>` so that `BINARY(16)` vs `UUID`
///   column definitions are cleanly separated.
///
/// Modeling these as a sealed interface makes the exhaustive set of
/// supported variants statically known to the compiler. A `switch` expression
/// over a `StorageDialect` value is exhaustively checked without a `default`
/// arm, so adding a new vendor forces every call-site to be updated.
///
/// ## Implementations
///
/// - [MySql]: used for [StorageType#MYSQL] and [StorageType#H2] (H2 runs in
///   `MODE=MySQL` and therefore shares the same DDL and binding strategy).
/// - [PostgreSql]: used for [StorageType#POSTGRESQL].
///
/// ## Thread safety
///
/// Both record implementations are stateless value objects. All methods
/// return freshly constructed objects or primitives; no shared mutable state
/// exists. Safe to call from any thread.
@NullMarked
public sealed interface StorageDialect permits StorageDialect.MySql, StorageDialect.PostgreSql {

    /// Returns the Flyway classpath location containing the versioned
    /// migration scripts for this dialect.
    ///
    /// @return the classpath location string, e.g. `"storage/migration/mysql"`
    String migrationLocation();

    /// Returns a JDBI [QualifiedArgumentFactory] that binds [UUID] values
    /// to SQL parameters in the format expected by this dialect's schema.
    ///
    /// @return a stateless [QualifiedArgumentFactory]; may be registered as
    ///         a singleton on the JDBI instance
    QualifiedArgumentFactory uuidArgumentFactory();

    /// Returns a JDBI [RowMapper] that reads a [UserProfile] from a SQL
    /// result row produced by this dialect's schema.
    ///
    /// @return a stateless [RowMapper]; may be registered as a singleton
    ///         on the JDBI instance
    RowMapper<UserProfile> profileMapper();

    // -------------------------------------------------------------------------
    // Private UUID helpers (shared by MySql implementation)
    // -------------------------------------------------------------------------

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

    // =========================================================================
    // MySQL / H2 dialect
    // =========================================================================

    /// Dialect for MySQL (external server) and H2 (embedded, `MODE=MySQL`).
    ///
    /// UUIDs are stored as `BINARY(16)`: the most-significant 8 bytes
    /// followed by the least-significant 8 bytes, big-endian. This matches
    /// the layout produced by [UUID#getMostSignificantBits()] and
    /// [UUID#getLeastSignificantBits()] and is reproduced exactly
    /// by [StorageDialect#uuidToBytes].
    ///
    /// Migration scripts are resolved from storage/migration/mysql`.
    @NullMarked
    record MySql() implements StorageDialect {

        private static final String LOCATION = "storage/migration/mysql";

        @Override
        public String migrationLocation() {
            return LOCATION;
        }

        /// Returns a [QualifiedArgumentFactory] that serializes a [UUID] to a
        /// 16-byte big-endian array and binds it with
        /// [java.sql.PreparedStatement#setBytes].
        @Override
        public QualifiedArgumentFactory uuidArgumentFactory() {
            return (_, value, _) -> {
                if (!(value instanceof final UUID uuid)) {
                    return Optional.empty();
                }
                final byte[] bytes = uuidToBytes(uuid);
                return Optional.of((position, statement, _) -> statement.setBytes(position, bytes));
            };
        }

        /// Returns a [RowMapper] that reads the `uuid` column as a 16-byte
        /// array via [java.sql.ResultSet#getBytes] and reconstructs the
        /// [UUID] from the big-endian bit layout.
        @Override
        public RowMapper<UserProfile> profileMapper() {
            return (rs, _) -> new UserProfile(
                    uuidFromBytes(rs.getBytes("uuid")),
                    rs.getString("name"),
                    Instant.ofEpochMilli(rs.getLong("last_seen"))
            );
        }
    }

    // =========================================================================
    // PostgreSQL dialect
    // =========================================================================

    /// Dialect for PostgreSQL.
    ///
    /// UUIDs are stored using PostgreSQL's native `uuid` column type.
    /// The PostgreSQL JDBC driver transparently maps `java.util.UUID`
    /// objects to and from the wire format when
    /// [java.sql.PreparedStatement#setObject] and
    /// [java.sql.ResultSet#getObject] are used with the correct class
    /// hint.
    ///
    /// Benefits over `BINARY(16)`:
    ///
    /// - No byte-order encoding/decoding required.
    /// - Native B-tree and hash indexes operate directly on the UUID value.
    /// - `EXPLAIN` output shows human-readable UUIDs rather than hex blobs.
    ///
    /// Migration scripts are resolved from `storage/migration/postgresql`.
    @NullMarked
    record PostgreSql() implements StorageDialect {

        private static final String LOCATION = "storage/migration/postgresql";

        @Override
        public String migrationLocation() {
            return LOCATION;
        }

        /// Returns a [QualifiedArgumentFactory] that binds a [UUID] value via
        /// [java.sql.PreparedStatement#setObject], delegating the wire-format
        /// serialization to the PostgreSQL JDBC driver.
        @Override
        public QualifiedArgumentFactory uuidArgumentFactory() {
            return (_, value, _) -> {
                if (!(value instanceof UUID)) {
                    return Optional.empty();
                }
                return Optional.of((position, statement, _) -> statement.setObject(position, value));
            };
        }

        /// Returns a [RowMapper] that reads the `uuid` column via
        /// [java.sql.ResultSet#getObject(String, Class)] with [UUID] as the
        /// target type, relying on the PostgreSQL JDBC driver for
        /// deserialization.
        @Override
        public RowMapper<UserProfile> profileMapper() {
            return (rs, _) -> new UserProfile(
                    rs.getObject("uuid", UUID.class),
                    rs.getString("name"),
                    Instant.ofEpochMilli(rs.getLong("last_seen"))
            );
        }
    }
}

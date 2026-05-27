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

import com.zaxxer.hikari.HikariDataSource;
import io.github.namiuni.paperplugintemplate.common.infrastructure.storage.DatabaseMigrator;
import io.github.namiuni.paperplugintemplate.common.infrastructure.storage.StorageDialect;
import io.github.namiuni.paperplugintemplate.common.utilities.UUIDCodec;
import jakarta.inject.Inject;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jspecify.annotations.NullMarked;

@NullMarked
public final class JdbiUserRepository implements UserRepository {

    private static final String UUID = "uuid";
    private static final String NAME = "name";
    private static final String LAST_SEEN = "last_seen";

    private static final String SQL_FIND_BY_UUID = "SELECT uuid, name, last_seen FROM users WHERE uuid = :uuid";
    private static final String SQL_INSERT = "INSERT INTO users (uuid, name, last_seen) VALUES (:uuid, :name, :lastSeen)";
    private static final String SQL_UPDATE = "UPDATE users SET name = :name, last_seen = :lastSeen WHERE uuid = :uuid";
    private static final String SQL_DELETE = "DELETE FROM users WHERE uuid = :uuid";

    private final ComponentLogger logger;
    private final Jdbi jdbi;
    private final HikariDataSource dataSource;
    private final RowMapper<UserRecord> rowMapper;

    @Inject
    JdbiUserRepository(
            final ComponentLogger logger,
            final Jdbi jdbi,
            final HikariDataSource dataSource,
            final StorageDialect dialect,
            final DatabaseMigrator migrator
    ) {
        this.logger = logger;
        this.jdbi = jdbi;
        this.dataSource = dataSource;
        this.rowMapper = rowMapperFor(dialect);
        migrator.migrate();
    }

    @Override
    public Optional<UserRecord> findById(final UUID uuid) {
        return this.jdbi.withHandle(handle -> handle
                .createQuery(SQL_FIND_BY_UUID)
                    .bind(UUID, uuid)
                    .map(this.rowMapper)
                .findFirst()
        );
    }

    @Override
    public void upsert(final UserRecord record) {
        this.jdbi.useTransaction(handle -> {
            final int updated = handle.createUpdate(SQL_UPDATE)
                    .bindMethods(record)
                    .execute();
            if (updated == 0) {
                try {
                    handle.createUpdate(SQL_INSERT)
                            .bindMethods(record)
                            .execute();
                } catch (final Exception _) {
                    handle.createUpdate(SQL_UPDATE)
                            .bindMethods(record)
                            .execute();
                }
            }
        });
    }

    @Override
    public void delete(final UUID uuid) {
        this.jdbi.useHandle(handle -> {
            handle.createUpdate(SQL_DELETE)
                    .bind(UUID, uuid)
                    .execute();
        });
    }

    @Override
    public void close() {
        this.logger.info("Closing HikariCP connection pool...");
        this.dataSource.close();
        this.logger.info("Connection pool closed.");
    }

    private static RowMapper<UserRecord> rowMapperFor(final StorageDialect dialect) {
        return switch (dialect) {
            case StorageDialect.MySQL() -> (rs, _) -> new UserRecord(
                    UUIDCodec.uuidFromBytes(rs.getBytes(UUID)),
                    rs.getString(NAME),
                    Instant.ofEpochMilli(rs.getLong(LAST_SEEN))
            );
            case StorageDialect.PostgreSQL() -> (rs, _) -> new UserRecord(
                    rs.getObject(UUID, UUID.class),
                    rs.getString(NAME),
                    Instant.ofEpochMilli(rs.getLong(LAST_SEEN))
            );
        };
    }
}

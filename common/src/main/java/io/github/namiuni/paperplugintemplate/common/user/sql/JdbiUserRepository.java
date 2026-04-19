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
package io.github.namiuni.paperplugintemplate.common.user.sql;

import com.zaxxer.hikari.HikariDataSource;
import io.github.namiuni.paperplugintemplate.common.Metadata;
import io.github.namiuni.paperplugintemplate.common.infrastructure.storage.DatabaseMigrator;
import io.github.namiuni.paperplugintemplate.common.infrastructure.storage.StorageDialect;
import io.github.namiuni.paperplugintemplate.common.user.UserRecord;
import io.github.namiuni.paperplugintemplate.common.user.UserRepository;
import io.github.namiuni.paperplugintemplate.common.utilities.UUIDCodec;
import jakarta.inject.Inject;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.async.JdbiExecutor;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jspecify.annotations.NullMarked;

@NullMarked
public final class JdbiUserRepository implements UserRepository {

    private static final String SQL_FIND_BY_UUID =
            "SELECT uuid, name, last_seen FROM users WHERE uuid = :uuid";
    private static final String SQL_INSERT =
            "INSERT INTO users (uuid, name, last_seen) VALUES (:uuid, :name, :lastSeen)";
    private static final String SQL_UPDATE =
            "UPDATE users SET name = :name, last_seen = :lastSeen WHERE uuid = :uuid";
    private static final String SQL_DELETE =
            "DELETE FROM users WHERE uuid = :uuid";

    private final JdbiExecutor jdbi;
    private final HikariDataSource dataSource;
    private final RowMapper<UserRecord> rowMapper;
    private final ComponentLogger logger;

    @Inject
    JdbiUserRepository(
            final Jdbi jdbi,
            final HikariDataSource dataSource,
            final StorageDialect dialect,
            final DatabaseMigrator migrator,
            final ComponentLogger logger,
            final Metadata metadata
    ) {
        final Executor executor = Executors.newThreadPerTaskExecutor(
                Thread.ofVirtual().name(metadata.name() + "-JDBI-User-Pool", 0).factory()
        );
        this.jdbi = JdbiExecutor.create(jdbi, executor);
        this.dataSource = dataSource;
        this.rowMapper = rowMapperFor(dialect);
        this.logger = logger;
        migrator.migrate();
    }

    @Override
    public CompletableFuture<Optional<UserRecord>> findById(final UUID uuid) {
        return this.jdbi.withHandle(handle -> {
            final Optional<UserRecord> result = handle.createQuery(SQL_FIND_BY_UUID)
                    .bind("uuid", uuid)
                    .map(this.rowMapper)
                    .findFirst();
            this.logger.debug("[{}] findById({}): {}", JdbiUserRepository.class.getSimpleName(), uuid, result);
            return result;
        }).toCompletableFuture();
    }

    @Override
    public CompletableFuture<Void> upsert(final UserRecord record) {
        return this.jdbi.useHandle(handle -> {
            this.logger.debug("[{}] upsert: {}", JdbiUserRepository.class.getSimpleName(), record);
            handle.useTransaction(tx -> {
                final int updated = tx.createUpdate(SQL_UPDATE)
                        .bindMethods(record)
                        .execute();
                if (updated == 0) {
                    try {
                        tx.createUpdate(SQL_INSERT)
                                .bindMethods(record)
                                .execute();
                    } catch (final Exception _) {
                        tx.createUpdate(SQL_UPDATE)
                                .bindMethods(record)
                                .execute();
                    }
                }
            });
        }).toCompletableFuture();
    }

    @Override
    public CompletableFuture<Void> delete(final UUID uuid) {
        return this.jdbi.useHandle(handle -> {
            this.logger.debug("[{}] delete({})", JdbiUserRepository.class.getSimpleName(), uuid);
            handle.createUpdate(SQL_DELETE)
                    .bind("uuid", uuid)
                    .execute();
        }).toCompletableFuture();
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
                    UUIDCodec.uuidFromBytes(rs.getBytes("uuid")),
                    rs.getString("name"),
                    Instant.ofEpochMilli(rs.getLong("last_seen"))
            );
            case StorageDialect.PostgreSQL() -> (rs, _) -> new UserRecord(
                    rs.getObject("uuid", UUID.class),
                    rs.getString("name"),
                    Instant.ofEpochMilli(rs.getLong("last_seen"))
            );
        };
    }
}

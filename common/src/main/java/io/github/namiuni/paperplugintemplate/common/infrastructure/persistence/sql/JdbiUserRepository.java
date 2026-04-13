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
package io.github.namiuni.paperplugintemplate.common.infrastructure.persistence.sql;

import com.zaxxer.hikari.HikariDataSource;
import io.github.namiuni.paperplugintemplate.common.Metadata;
import io.github.namiuni.paperplugintemplate.common.infrastructure.persistence.FlywayLogger;
import io.github.namiuni.paperplugintemplate.common.infrastructure.persistence.UserRecord;
import io.github.namiuni.paperplugintemplate.common.infrastructure.persistence.UserRepository;
import jakarta.inject.Inject;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.logging.LogFactory;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.async.JdbiExecutor;
import org.jspecify.annotations.NullMarked;

/// [UserRepository] implementation backed by a SQL database via JDBI 3.
///
/// Supports H2 (in `MODE=MySQL`) and MySQL/MariaDB using identical SQL
/// statements. Vendor-specific upsert syntax such as `ON DUPLICATE KEY UPDATE`
/// is avoided; the portable update-then-insert strategy is encapsulated entirely
/// in [UserDao#upsert], keeping this class free of SQL dialect concerns.
///
/// ## Ownership and lifecycle
///
/// The [HikariDataSource] passed at construction is **owned** by this
/// repository. Call [#close()] on plugin disable to release all pooled JDBC
/// connections and stop the HikariCP housekeeping thread.
///
/// ## Thread safety
///
/// All operations are submitted to a virtual-thread-per-task executor. No
/// `synchronized` blocks are used; this class is safe from carrier-thread
/// pinning (JEP 491).
@NullMarked
public final class JdbiUserRepository implements UserRepository, AutoCloseable {

    private final JdbiExecutor jdbi;
    private final HikariDataSource dataSource;
    private final ComponentLogger logger;

    /// Constructs a new repository.
    ///
    /// @param jdbi         the configured JDBI instance with all required plugins and row mappers installed
    /// @param dataSource   the HikariCP connection pool; this repository takes ownership and closes it on [#close()]
    /// @param flyway       the Flyway instance managing schema migrations
    /// @param flywayLogger the logger adapter that routes Flyway output to the plugin logger
    /// @param logger       the component-aware logger
    @Inject
    private JdbiUserRepository(
            final Jdbi jdbi,
            final HikariDataSource dataSource,
            final Flyway flyway,
            final FlywayLogger flywayLogger,
            final ComponentLogger logger,
            final Metadata metadata
    ) {
        final Executor virtualExecutor = Executors.newThreadPerTaskExecutor(
                Thread.ofVirtual().name(metadata.name() + "-JDBI-User-Pool", 0).factory()
        );
        this.jdbi = JdbiExecutor.create(jdbi, virtualExecutor);
        this.dataSource = dataSource;
        this.logger = logger;

        this.logger.info("Running Flyway repair & migration...");
        LogFactory.setLogCreator(flywayLogger);
        try {
            flyway.repair();
            final var result = flyway.migrate();
            if (result.migrationsExecuted == 0) {
                this.logger.info("Schema is up-to-date. No migrations applied.");
            } else {
                this.logger.info(
                        "Applied {} migration(s). Schema is now at version {}.",
                        result.migrationsExecuted,
                        result.targetSchemaVersion
                );
            }
        } finally {
            LogFactory.setLogCreator(null);
        }
    }

    /// {@inheritDoc}
    @Override
    public CompletableFuture<Optional<UserRecord>> findById(final UUID uuid) {
        this.logger.debug("[SQL] findById: {}", uuid);
        return this.jdbi.withExtension(UserDao.class, dao -> dao.findByUuid(uuid))
                .toCompletableFuture();
    }

    /// {@inheritDoc}
    @Override
    public CompletableFuture<Void> upsert(final UserRecord userRecord) {
        this.logger.debug("[SQL] upsert: {} ({})", userRecord.uuid(), userRecord.name());
        return this.jdbi.useExtension(UserDao.class, dao -> dao.upsert(userRecord))
                .toCompletableFuture();
    }

    /// {@inheritDoc}
    @Override
    public CompletableFuture<Void> delete(final UUID uuid) {
        this.logger.debug("[SQL] delete: {}", uuid);
        return this.jdbi.useExtension(UserDao.class, dao -> dao.deleteByUuid(uuid))
                .toCompletableFuture();
    }

    /// Closes the underlying [HikariDataSource], terminating all pooled
    /// connections and the HikariCP housekeeping thread.
    ///
    /// Must be called during plugin disable. Subsequent calls to any other
    /// method on this instance will result in undefined behavior.
    @Override
    public void close() {
        this.logger.info("Closing HikariCP connection pool...");
        this.dataSource.close();
        this.logger.info("Connection pool closed.");
    }
}

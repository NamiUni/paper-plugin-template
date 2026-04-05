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

import com.zaxxer.hikari.HikariDataSource;
import io.github.namiuni.paperplugintemplate.common.user.storage.FlywayLogger;
import io.github.namiuni.paperplugintemplate.common.user.storage.UserProfile;
import io.github.namiuni.paperplugintemplate.common.user.storage.UserRepository;
import jakarta.inject.Inject;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
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
/// ## Schema management
///
/// The database schema is fully managed by Flyway, which runs migrations
/// before this repository is used. [#initialize()] is therefore a no-op for
/// SQL backends.
///
/// ## Ownership and lifecycle
///
/// The [HikariDataSource] passed at construction is **owned** by this
/// repository. Call [#close()] on plugin disable to release all pooled JDBC
/// connections and stop the HikariCP housekeeping thread.
///
/// ## Thread safety
///
/// All operations are submitted to a virtual-thread-per-task executor named
/// `YourPlugin-DB-User-Repo-N`. The [JdbiExecutor] wraps JDBI's handle
/// lifecycle so that each operation acquires and releases a connection within
/// its virtual thread without blocking the caller. No `synchronized` blocks are
/// used; this class is safe from carrier-thread pinning (JEP 491).
///
/// @implNote HikariCP connection acquisition may block briefly if all
///           connections are in use. On virtual threads this parks the virtual thread
///           rather than blocking the carrier thread, provided HikariCP 5.0+ is used.
@NullMarked
public final class JdbiUserRepository implements UserRepository, AutoCloseable {

    private static final Executor VIRTUAL_EXECUTOR = Executors.newThreadPerTaskExecutor(
            Thread.ofVirtual().name("PaperPluginTemplate-JDBI-User-Pool", 0).factory()
    );

    private final JdbiExecutor jdbi;
    private final HikariDataSource dataSource;
    private final Flyway flyway;
    private final FlywayLogger flywayLogger;

    /// Constructs a new repository.
    ///
    /// @param jdbi       the configured JDBI instance with all required plugins and row mappers installed
    /// @param dataSource the HikariCP connection pool; this repository takes ownership and closes it on [#close()]
    @Inject
    private JdbiUserRepository(
            final Jdbi jdbi,
            final HikariDataSource dataSource,
            final Flyway flyway,
            final FlywayLogger flywayLogger
    ) {
        this.jdbi = JdbiExecutor.create(jdbi, VIRTUAL_EXECUTOR);
        this.dataSource = dataSource;
        this.flyway = flyway;
        this.flywayLogger = flywayLogger;
    }

    /// No-op for SQL backends.
    ///
    /// The `users` table and all subsequent schema versions are created by
    /// Flyway during injector construction (see `StorageModule.jdbi()`).
    /// By the time this method is called from
    /// [io.github.namiuni.paperplugintemplate.common.PluginInternal#initialize()],
    /// the schema is already up-to-date and no DDL work remains.
    @Override
    public void initialize() {
        LogFactory.setLogCreator(this.flywayLogger);
        this.flyway.repair();
        this.flyway.migrate();
        LogFactory.setLogCreator(null);
    }

    /// {@inheritDoc}
    @Override
    public CompletableFuture<Optional<UserProfile>> findById(final UUID uuid) {
        return this.jdbi.withExtension(UserDao.class, dao -> dao.findByUuid(uuid))
                .toCompletableFuture();
    }

    /// {@inheritDoc}
    @Override
    public CompletableFuture<Void> upsert(final UserProfile userProfile) {
        return this.jdbi.useExtension(UserDao.class, dao -> dao.upsert(userProfile))
                .toCompletableFuture();
    }

    /// {@inheritDoc}
    @Override
    public CompletableFuture<Void> delete(final UUID uuid) {
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
        this.dataSource.close();
    }
}

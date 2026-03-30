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

import com.zaxxer.hikari.HikariDataSource;
import io.github.namiuni.paperplugintemplate.user.storage.UserProfile;
import io.github.namiuni.paperplugintemplate.user.storage.UserRepository;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.async.JdbiExecutor;
import org.jspecify.annotations.NullMarked;

/// [UserRepository] implementation backed by a SQL database via JDBI.
///
/// Supports H2 (`MODE=MySQL`) and MySQL using identical SQL statements.
/// Database-vendor-specific upsert syntax is avoided; the portable
/// update-then-insert strategy is encapsulated in [UserDao#upsert(UserProfile)].
///
/// The [HikariDataSource] is owned by this repository. Call [#close()] during
/// plugin disable to release all pooled connections; no other method may be called
/// afterwards.
@NullMarked
public final class JdbiUserRepository implements UserRepository, AutoCloseable {

    private static final Executor VIRTUAL_EXECUTOR = Executors.newThreadPerTaskExecutor(
            Thread.ofVirtual().name("YourPlugin-DB-User-Repo-", 0).factory()
    );

    private final JdbiExecutor jdbi;
    private final HikariDataSource dataSource;

    /// Constructs a new repository.
    ///
    /// @param jdbi       the configured JDBI instance with all required plugins and
    ///                   mappers already installed
    /// @param dataSource the connection pool; this repository takes ownership and will
    ///                   close it via [#close()]
    public JdbiUserRepository(final Jdbi jdbi, final HikariDataSource dataSource) {
        this.jdbi = JdbiExecutor.create(jdbi, VIRTUAL_EXECUTOR);
        this.dataSource = dataSource;
    }

    /// {@inheritDoc}
    @Override
    public void initialize() {
        this.jdbi.useExtension(UserDao.class, UserDao::createTable);
    }

    /// {@inheritDoc}
    @Override
    public CompletableFuture<Optional<UserProfile>> findById(final UUID uuid) {
        return this.jdbi.withExtension(UserDao.class, dao -> dao.findByUuid(uuid)).toCompletableFuture();
    }

    /// {@inheritDoc}
    @Override
    public CompletableFuture<Void> upsert(final UserProfile userProfile) {
        return this.jdbi.useExtension(UserDao.class, dao -> dao.upsert(userProfile)).toCompletableFuture();
    }

    /// {@inheritDoc}
    @Override
    public CompletableFuture<Void> delete(final UUID uuid) {
        return this.jdbi.useExtension(UserDao.class, dao -> dao.deleteByUuid(uuid)).toCompletableFuture();
    }

    /// Closes the underlying [HikariDataSource], terminating all pooled connections.
    ///
    /// Must be called exactly once during plugin disable.
    @Override
    public void close() {
        this.dataSource.close();
    }
}

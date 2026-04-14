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

@NullMarked
public final class JdbiUserRepository implements UserRepository {

    private final JdbiExecutor jdbi;
    private final HikariDataSource dataSource;
    private final ComponentLogger logger;

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
            flyway.migrate();
        } finally {
            LogFactory.setLogCreator(null);
        }
    }

    @Override
    public CompletableFuture<Optional<UserRecord>> findById(final UUID uuid) {
        return this.jdbi.withExtension(UserDao.class, dao -> {
            final var existing = dao.findByUuid(uuid);
            this.logger.debug("[{}] findById: {}", JdbiUserRepository.class.getSimpleName(), existing);
            return existing;
        }).toCompletableFuture();
    }

    @Override
    public CompletableFuture<Void> upsert(final UserRecord userRecord) {
        return this.jdbi.useExtension(UserDao.class, dao -> {
            this.logger.debug("[{}] upsert: {}", JdbiUserRepository.class.getSimpleName(), userRecord);
            dao.upsert(userRecord);
        }).toCompletableFuture();
    }

    @Override
    public CompletableFuture<Void> delete(final UUID uuid) {
        return this.jdbi.useExtension(UserDao.class, dao -> {
            this.logger.debug("[{}] delete: {}", JdbiUserRepository.class.getSimpleName(), uuid);
            dao.deleteByUuid(uuid);
        }).toCompletableFuture();
    }

    @Override
    public void close() {
        this.logger.info("Closing HikariCP connection pool...");
        this.dataSource.close();
        this.logger.info("Connection pool closed.");
    }
}

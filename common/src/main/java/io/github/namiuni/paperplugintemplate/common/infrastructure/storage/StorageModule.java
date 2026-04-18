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
package io.github.namiuni.paperplugintemplate.common.infrastructure.storage;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.github.namiuni.paperplugintemplate.api.PluginTemplate;
import io.github.namiuni.paperplugintemplate.common.Metadata;
import io.github.namiuni.paperplugintemplate.common.infrastructure.DataDirectory;
import io.github.namiuni.paperplugintemplate.common.infrastructure.configuration.configurations.PrimaryConfiguration;
import io.github.namiuni.paperplugintemplate.common.infrastructure.storage.json.JsonUserRepository;
import io.github.namiuni.paperplugintemplate.common.infrastructure.storage.sql.JdbiUserRepository;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;
import java.util.function.Consumer;
import org.flywaydb.core.Flyway;
import org.jdbi.v3.cache.caffeine.CaffeineCacheBuilder;
import org.jdbi.v3.cache.caffeine.CaffeineCachePlugin;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.argument.QualifiedArgumentFactory;
import org.jdbi.v3.core.statement.SqlStatements;
import org.jdbi.v3.postgres.PostgresPlugin;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.jspecify.annotations.NullMarked;

@NullMarked
public final class StorageModule extends AbstractModule {

    @Provides
    @Singleton
    @SuppressWarnings("unused")
    UserRepository userRepository(
            final Provider<PrimaryConfiguration> config,
            final Provider<JsonUserRepository> json,
            final Provider<JdbiUserRepository> jdbi
    ) {
        return switch (config.get().storage().type()) {
            case JSON -> json.get();
            case H2, MYSQL, POSTGRESQL -> jdbi.get();
        };
    }

    @Provides
    @Singleton
    @SuppressWarnings("unused")
    HikariDataSource dataSource(
            final Provider<PrimaryConfiguration> primaryConfig,
            final @DataDirectory Path dataDirectory,
            final Metadata metadata
    ) {
        final PrimaryConfiguration.Storage storage = primaryConfig.get().storage();
        final PrimaryConfiguration.Storage.Pool pool = storage.pool();
        final HikariConfig config = new HikariConfig();
        config.setPoolName(metadata.name());
        config.setMaximumPoolSize(pool.maximumPoolSize());
        config.setMinimumIdle(pool.minimumIdle());
        config.setMaxLifetime(pool.maximumLifetime());
        config.setKeepaliveTime(pool.keepaliveTime());
        config.setConnectionTimeout(pool.connectionTimeout());
        config.setThreadFactory(Thread.ofVirtual().name(metadata.name() + "-Hikari-Pool", 0).factory());

        switch (storage.type()) {
            case H2 -> {
                final Path dbFile = dataDirectory.toAbsolutePath().resolve("database");
                config.setJdbcUrl("jdbc:h2:file:%s;MODE=MySQL;DB_CLOSE_DELAY=-1".formatted(dbFile));
                config.setDriverClassName("org.h2.Driver");
            }
            case MYSQL -> {
                config.setJdbcUrl("jdbc:mysql://%s:%d/%s?useSSL=false&autoReconnect=true&characterEncoding=utf8"
                        .formatted(storage.host(), storage.port(), storage.database()));
                config.setUsername(storage.username());
                config.setPassword(storage.password());
                config.setDriverClassName("com.mysql.cj.jdbc.Driver");
            }
            case POSTGRESQL -> {
                config.setJdbcUrl("jdbc:postgresql://%s:%d/%s"
                        .formatted(storage.host(), storage.port(), storage.database()));
                config.setUsername(storage.username());
                config.setPassword(storage.password());
                config.setDriverClassName("org.postgresql.Driver");
            }
            default -> throw new IllegalStateException("Unexpected SQL storage type: " + storage.type());
        }

        return new HikariDataSource(config);
    }

    @Provides
    @Singleton
    @SuppressWarnings("unused")
    StorageDialect storageDialect(final Provider<PrimaryConfiguration> primaryConfig) {
        return switch (primaryConfig.get().storage().type()) {
            case H2, MYSQL -> new StorageDialect.MySQL();
            case POSTGRESQL -> new StorageDialect.PostgreSQL();
            case JSON -> throw new IllegalArgumentException("StorageType.JSON has no SQL dialect");
        };
    }

    @Provides
    @Singleton
    @SuppressWarnings("unused")
    Jdbi jdbi(final HikariDataSource dataSource, final StorageDialect dialect) {
        final QualifiedArgumentFactory instantArgument = (_, value, _) -> {
            if (!(value instanceof final Instant instant)) {
                return Optional.empty();
            }
            return Optional.of((position, statement, _) -> statement.setLong(position, instant.toEpochMilli()));
        };

        final Consumer<SqlStatements> caffeineCache = config ->
                config.setTemplateCache(new CaffeineCacheBuilder(Caffeine.newBuilder()));

        final Jdbi jdbi = Jdbi.create(dataSource)
                .installPlugin(new SqlObjectPlugin())
                .installPlugin(new CaffeineCachePlugin())
                .registerRowMapper(UserRecord.class, dialect.profileMapper())
                .registerArgument(dialect.uuidArgumentFactory())
                .registerArgument(instantArgument)
                .configure(SqlStatements.class, caffeineCache);

        if (dialect instanceof StorageDialect.PostgreSQL) {
            jdbi.installPlugin(new PostgresPlugin());
        }

        return jdbi;
    }

    @Provides
    @Singleton
    @SuppressWarnings("unused")
    Flyway flyway(final HikariDataSource dataSource, final StorageDialect dialect) {
        return Flyway.configure(PluginTemplate.class.getClassLoader())
                .baselineVersion("0")
                .baselineOnMigrate(true)
                .locations(dialect.migrationLocation())
                .dataSource(dataSource)
                .validateMigrationNaming(true)
                .validateOnMigrate(true)
                .load();
    }
}

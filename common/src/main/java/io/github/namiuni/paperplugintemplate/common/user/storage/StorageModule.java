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

import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.github.namiuni.paperplugintemplate.api.PluginTemplate;
import io.github.namiuni.paperplugintemplate.common.DataDirectory;
import io.github.namiuni.paperplugintemplate.common.configuration.PrimaryConfiguration;
import io.github.namiuni.paperplugintemplate.common.user.storage.json.JsonUserRepository;
import io.github.namiuni.paperplugintemplate.common.user.storage.sql.JdbiUserRepository;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.flywaydb.core.Flyway;
import org.jdbi.v3.cache.caffeine.CaffeineCacheBuilder;
import org.jdbi.v3.cache.caffeine.CaffeineCachePlugin;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.argument.QualifiedArgumentFactory;
import org.jdbi.v3.core.statement.SqlStatements;
import org.jdbi.v3.postgres.PostgresPlugin;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.jspecify.annotations.NullMarked;

/// Guice module responsible for selecting and binding the active
/// [UserRepository] based on the configured [StorageType].
///
/// ## Supported backends
///
/// | [StorageType] | Driver | UUID column | Migration location |
/// |---|---|---|---|
/// | `H2`         | `org.h2.Driver` (`MODE=MySQL`)     | `BINARY(16)` | `db/migration/mysql`      |
/// | `MYSQL`      | `com.mysql.cj.jdbc.Driver`         | `BINARY(16)` | `db/migration/mysql`      |
/// | `POSTGRESQL` | `org.postgresql.Driver`            | `uuid`       | `db/migration/postgresql` |
/// | `JSON`       | — (no JDBC)                        | —            | —                         |
///
/// ## Dialect-aware UUID handling
///
/// MySQL/H2 and PostgreSQL require fundamentally different UUID strategies.
/// This difference is modeled by [StorageDialect]: a sealed interface whose
/// two implementations ([StorageDialect.MySql] and [StorageDialect.PostgreSql])
/// each provide:
///
/// - a Flyway migration location,
/// - a JDBI [org.jdbi.v3.core.argument.QualifiedArgumentFactory] for binding UUIDs, and
/// - a JDBI [org.jdbi.v3.core.mapper.RowMapper] for reading [UserProfile] rows.
///
/// ## Schema management
///
/// For SQL backends, Flyway runs migrations before the [Jdbi] instance is
/// constructed. A failed migration throws a
/// [org.flywaydb.core.api.FlywayException] that propagates through Guice's
/// provider chain and aborts plugin startup cleanly.
///
/// ## Lazy providers
///
/// [Jdbi] and [HikariDataSource] providers are injected as [Provider]s in
/// [#userRepository] and are **never dereferenced** when [StorageType#JSON]
/// is selected, preventing any database connection from being opened.
@NullMarked
@SuppressWarnings("unused")
public final class StorageModule extends AbstractModule {

    /// Constructs a new `StorageModule`.
    public StorageModule() {
    }

    /// Provides the singleton [UserRepository] for the configured [StorageType].
    ///
    /// @param primaryConfig the configuration holder used to determine the active [StorageType]
    /// @param jsonRepository lazy provider for the JSON user repository instance
    /// @param jdbiRepository lazy provider for the JDBI user repository instance
    /// @return the initialized [UserRepository] singleton
    @Provides
    @Singleton
    private UserRepository userRepository(
            final Supplier<PrimaryConfiguration> primaryConfig,
            final Provider<JsonUserRepository> jsonRepository,
            final Provider<JdbiUserRepository> jdbiRepository
    ) {
        final PrimaryConfiguration.Storage storage = primaryConfig.get().storage();
        return switch (storage.type()) {
            case JSON -> jsonRepository.get();
            case H2, MYSQL, POSTGRESQL -> jdbiRepository.get();
        };
    }

    /// Provides a singleton [Jdbi] instance configured for the active [StorageDialect].
    ///
    /// The dialect is resolved from the storage configuration and determines:
    ///
    /// - which [org.jdbi.v3.core.argument.QualifiedArgumentFactory] handles [java.util.UUID]
    ///   binding, and
    /// - which [org.jdbi.v3.core.mapper.RowMapper] reads [UserProfile] rows.
    ///
    /// @param dataSource the HikariCP pool
    /// @param dialect    the `StorageDialect`
    /// @return a fully configured `Jdbi` singleton
    @Provides
    @Singleton
    private Jdbi jdbi(
            final HikariDataSource dataSource,
            final StorageDialect dialect
    ) {
        final QualifiedArgumentFactory instantArgument = (_, value, _) -> {
            if (!(value instanceof final Instant instant)) {
                return Optional.empty();
            }
            return Optional.of((position, statement, _) -> statement.setLong(position, instant.toEpochMilli()));
        };

        final Consumer<SqlStatements> caffeineCache = config -> config.setTemplateCache(
                new CaffeineCacheBuilder(Caffeine.newBuilder())
        );

        final Jdbi jdbi = Jdbi.create(dataSource)
                .installPlugin(new SqlObjectPlugin())
                .installPlugin(new CaffeineCachePlugin())
                .registerRowMapper(UserProfile.class, dialect.profileMapper())
                .registerArgument(dialect.uuidArgumentFactory())
                .registerArgument(instantArgument)
                .configure(SqlStatements.class, caffeineCache);

        if (dialect instanceof StorageDialect.PostgreSql) {
            jdbi.installPlugin(new PostgresPlugin());
        }

        return jdbi;
    }

    @Provides
    @Singleton
    private StorageDialect storageDialect(final Supplier<PrimaryConfiguration> primaryConfig) {
        return switch (primaryConfig.get().storage().type()) {
            case H2, MYSQL -> new StorageDialect.MySql();
            case POSTGRESQL -> new StorageDialect.PostgreSql();
            case JSON -> throw new IllegalArgumentException("StorageType.JSON has no SQL dialect");
        };
    }

    @Provides
    @Singleton
    private Flyway flyway(
            final HikariDataSource dataSource,
            final StorageDialect dialect
    ) {
        return Flyway.configure(PluginTemplate.class.getClassLoader())
                .baselineVersion("0")
                .baselineOnMigrate(true)
                .locations(dialect.migrationLocation())
                .dataSource(dataSource)
                .validateMigrationNaming(true)
                .validateOnMigrate(true)
                .load();
    }

    /// Provides a singleton [HikariDataSource] for SQL storage backends.
    ///
    /// H2 is opened with `MODE=MySQL;DB_CLOSE_DELAY=-1` to share the same DDL
    /// and UUID binding strategy as the MySQL backend. PostgreSQL uses a plain
    /// `jdbc:postgresql://` URL; no extra query parameters are required for
    /// basic UTF-8 connections.
    ///
    /// This provider is never called when [StorageType#JSON] is selected.
    ///
    /// @param primaryConfig the configuration holder supplying JDBC parameters
    /// @param dataDirectory the plugin data directory; used to resolve the H2
    ///        database file path
    /// @return a started [HikariDataSource] with the configured pool size
    /// @throws IllegalStateException if the storage type is [StorageType#JSON];
    ///         this should never occur because Guice only invokes this provider
    ///         when a SQL backend is active
    /// @implNote Replace the pool name `"YourPlugin"` with the actual plugin name
    ///           to avoid ambiguity in HikariCP thread dumps.
    @Provides
    @Singleton
    private HikariDataSource dataSource(
            final Supplier<PrimaryConfiguration> primaryConfig,
            final @DataDirectory Path dataDirectory
    ) {
        final PrimaryConfiguration.Storage storage = primaryConfig.get().storage();
        final PrimaryConfiguration.Storage.Pool pool = storage.pool();
        final HikariConfig config = new HikariConfig();
        config.setPoolName("PaperPluginTemplate"); // TODO: replace with the actual plugin name
        config.setMaximumPoolSize(pool.maximumPoolSize());
        config.setMinimumIdle(pool.minimumIdle());
        config.setMaxLifetime(pool.maximumLifetime());
        config.setKeepaliveTime(pool.keepaliveTime());
        config.setConnectionTimeout(pool.connectionTimeout());
        config.setThreadFactory(Thread.ofVirtual().name("PaperPluginTemplate-Hikari-Pool", 0).factory());

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
}

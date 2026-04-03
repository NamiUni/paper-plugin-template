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
package io.github.namiuni.paperplugintemplate.common.user.storage;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.inject.AbstractModule;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.github.namiuni.paperplugintemplate.common.DataDirectory;
import io.github.namiuni.paperplugintemplate.common.configuration.ConfigurationHolder;
import io.github.namiuni.paperplugintemplate.common.configuration.PrimaryConfiguration;
import io.github.namiuni.paperplugintemplate.common.user.storage.json.JsonUserRepository;
import io.github.namiuni.paperplugintemplate.common.user.storage.sql.JdbiUserRepository;
import io.github.namiuni.paperplugintemplate.common.user.storage.sql.UserProfileMapper;
import jakarta.inject.Singleton;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.jdbi.v3.cache.caffeine.CaffeineCacheBuilder;
import org.jdbi.v3.cache.caffeine.CaffeineCachePlugin;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.argument.QualifiedArgumentFactory;
import org.jdbi.v3.core.statement.SqlStatements;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.jspecify.annotations.NullMarked;

/// Guice module responsible for selecting and binding the active
/// [UserRepository] based on the configured [StorageType].
///
/// The [Jdbi] and [HikariDataSource] providers are lazy: they are only invoked
/// by Guice when a SQL backend ([StorageType#H2] or [StorageType#MYSQL]) is
/// selected. For [StorageType#JSON] these providers are never called, so no
/// database connection is ever opened.
///
/// ## Module responsibilities
///
/// This module does not call [UserRepository#initialize()] itself.
/// Initialization is performed by
/// [io.github.namiuni.paperplugintemplate.common.PluginInternal#initialize()]
/// after all bindings are resolved.
@NullMarked
@SuppressWarnings("unused")
public final class StorageModule extends AbstractModule {

    /// Constructs a new `StorageModule`.
    public StorageModule() {
    }

    /// Provides the singleton [UserRepository] for the configured [StorageType].
    ///
    /// The `jdbi` and `dataSource` parameters are lazy [Provider]s; they are
    /// only dereferenced when the storage type requires a SQL connection. This
    /// prevents HikariCP from opening connections when the JSON backend is
    /// selected.
    ///
    /// @param primaryConfig the configuration holder used to determine the active [StorageType]
    /// @param dataDirectory the plugin data directory; forwarded to [JsonUserRepository]
    /// @param jdbi          lazy provider for the JDBI instance; called only for SQL backends
    /// @param dataSource    lazy provider for the HikariCP pool; called only for SQL backends
    /// @return the initialized [UserRepository] singleton
    @Provides
    @Singleton
    private UserRepository userRepository(
            final ConfigurationHolder<PrimaryConfiguration> primaryConfig,
            final @DataDirectory Path dataDirectory,
            final Provider<Jdbi> jdbi,
            final Provider<HikariDataSource> dataSource
    ) {
        final PrimaryConfiguration.StorageConfig storageConfig = primaryConfig.get().storage();
        return switch (storageConfig.type()) {
            case JSON -> new JsonUserRepository(dataDirectory);
            case H2, MYSQL -> new JdbiUserRepository(jdbi.get(), dataSource.get());
        };
    }

    /// Provides a singleton [Jdbi] instance with all required plugins installed.
    ///
    /// Installed plugins and configuration:
    ///
    /// - `SqlObjectPlugin`: enables the `@Sql*` annotation-driven DAO pattern
    ///   used by [io.github.namiuni.paperplugintemplate.common.user.storage.sql.UserDao].
    /// - `CaffeineCachePlugin`: caches compiled SQL templates in a bounded
    ///   Caffeine cache (512 entries, 15-minute expiry) to avoid repeated
    ///   template-parsing overhead.
    /// - [UserProfileMapper]: maps SQL result rows to [io.github.namiuni.paperplugintemplate.common.user.storage.UserProfile] records.
    /// - `QualifiedArgumentFactory`: binds [Instant] values as
    ///   epoch-millisecond `BIGINT`s for cross-database compatibility.
    ///
    /// @param dataSource the HikariCP connection pool
    /// @return a fully configured [Jdbi] singleton
    @Provides
    @Singleton
    private Jdbi jdbi(final HikariDataSource dataSource) {
        return Jdbi.create(dataSource)
                .installPlugin(new SqlObjectPlugin())
                .installPlugin(new CaffeineCachePlugin())
                .registerRowMapper(new UserProfileMapper())
                .registerArgument((QualifiedArgumentFactory) (type, value, config) -> {
                    if (!(value instanceof final Instant instant)) {
                        return Optional.empty();
                    }
                    return Optional.of((position, statement, ctx) ->
                            statement.setLong(position, instant.toEpochMilli()));
                })
                .configure(SqlStatements.class, config -> config.setTemplateCache(
                        new CaffeineCacheBuilder(
                                Caffeine.newBuilder()
                                        .maximumSize(512L)
                                        .expireAfterAccess(15L, TimeUnit.MINUTES)
                        )));
    }

    /// Provides a singleton [HikariDataSource] for SQL storage backends.
    ///
    /// H2 is opened with `MODE=MySQL;DB_CLOSE_DELAY=-1` to share the same SQL
    /// dialect as the MySQL backend. This provider is never called when the
    /// storage type is [StorageType#JSON].
    ///
    /// @param primaryConfig the configuration holder supplying JDBC connection
    ///        parameters
    /// @param dataDirectory the plugin data directory; used to resolve the H2
    ///        database file path
    /// @return a started [HikariDataSource] with the configured pool size
    /// @throws IllegalStateException if the storage type is not a SQL variant;
    ///         this should never occur because this provider is only invoked for H2
    ///         and MySQL
    /// @implNote The pool name defaults to `YourPlugin`. Replace this with the
    ///           actual plugin name to avoid ambiguity in thread dumps.
    @Provides
    @Singleton
    private HikariDataSource dataSource(
            final ConfigurationHolder<PrimaryConfiguration> primaryConfig,
            final @DataDirectory Path dataDirectory
    ) {
        final PrimaryConfiguration.StorageConfig storageConfig = primaryConfig.get().storage();
        final HikariConfig config = new HikariConfig();
        config.setPoolName("YourPlugin"); // TODO: replace with the actual plugin name
        config.setMaximumPoolSize(storageConfig.maximumPoolSize());

        switch (storageConfig.type()) {
            case H2 -> {
                final Path dbFile = dataDirectory.toAbsolutePath().resolve("database");
                config.setJdbcUrl("jdbc:h2:file:%s;MODE=MySQL;DB_CLOSE_DELAY=-1"
                        .formatted(dbFile));
                config.setDriverClassName("org.h2.Driver");
            }
            case MYSQL -> {
                config.setJdbcUrl(
                        "jdbc:mysql://%s:%d/%s?useSSL=false&autoReconnect=true&characterEncoding=utf8"
                                .formatted(
                                        storageConfig.host(),
                                        storageConfig.port(),
                                        storageConfig.database()
                                ));
                config.setUsername(storageConfig.username());
                config.setPassword(storageConfig.password());
                config.setDriverClassName("com.mysql.cj.jdbc.Driver");
            }
            default -> throw new IllegalStateException(
                    "Unexpected SQL storage type: " + storageConfig.type());
        }

        return new HikariDataSource(config);
    }
}

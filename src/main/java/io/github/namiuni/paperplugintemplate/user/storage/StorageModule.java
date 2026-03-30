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
package io.github.namiuni.paperplugintemplate.user.storage;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.inject.AbstractModule;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.github.namiuni.paperplugintemplate.DataDirectory;
import io.github.namiuni.paperplugintemplate.configuration.ConfigurationHolder;
import io.github.namiuni.paperplugintemplate.configuration.PrimaryConfiguration;
import io.github.namiuni.paperplugintemplate.user.storage.json.JsonUserRepository;
import io.github.namiuni.paperplugintemplate.user.storage.sql.JdbiUserRepository;
import io.github.namiuni.paperplugintemplate.user.storage.sql.UserProfileMapper;
import jakarta.inject.Singleton;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.jdbi.v3.cache.caffeine.CaffeineCacheBuilder;
import org.jdbi.v3.cache.caffeine.CaffeineCachePlugin;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.statement.SqlStatements;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.jspecify.annotations.NullMarked;

/// Guice module that selects and wires the active [UserRepository] implementation.
///
/// The storage backend is chosen at injection time based on
/// [PrimaryConfiguration.StorageConfig#type()]:
///
///   - [StorageType#JSON] — [JsonUserRepository], no database required
///   - [StorageType#H2] — [JdbiUserRepository] backed by an embedded H2 file database
///   - [StorageType#MYSQL] — [JdbiUserRepository] backed by an external MySQL server
///
/// The [HikariDataSource] and [Jdbi] bindings are only materialized when the
/// active storage type is H2 or MySQL; switching to JSON avoids constructing a
/// connection pool entirely.
@NullMarked
@SuppressWarnings("unused")
public final class StorageModule extends AbstractModule {

    /// Constructs a new `StorageModule`.
    public StorageModule() {
    }

    /// Provides the singleton [UserRepository] based on the configured storage type.
    ///
    /// @param primaryConfig holder for the primary plugin configuration; used to read
    ///                      the active [PrimaryConfiguration.StorageConfig]
    /// @param dataDirectory the plugin data directory; forwarded to [JsonUserRepository]
    /// @param jdbi          lazy provider for the configured JDBI instance
    /// @param dataSource    lazy provider for the HikariCP connection pool
    /// @return the initialized repository selected for the configured storage type
    @Provides
    @Singleton
    private UserRepository userRepository(
            final ConfigurationHolder<PrimaryConfiguration> primaryConfig,
            final @DataDirectory Path dataDirectory,
            final Provider<Jdbi> jdbi,
            final Provider<HikariDataSource> dataSource
    ) {
        return switch (primaryConfig.get().storage().type()) {
            case JSON -> new JsonUserRepository(dataDirectory);
            case H2, MYSQL -> new JdbiUserRepository(jdbi.get(), dataSource.get());
        };
    }

    /// Provides a singleton [Jdbi] instance with all required plugins installed.
    ///
    /// The SQL-statement template cache is backed by Caffeine with a 15-minute
    /// access-expiry and a maximum size of 512 entries.
    ///
    /// @param dataSource the HikariCP connection pool
    /// @return a fully configured [Jdbi] instance
    @Provides
    @Singleton
    private Jdbi jdbi(final HikariDataSource dataSource) {
        return Jdbi.create(dataSource)
                .installPlugin(new SqlObjectPlugin())
                .installPlugin(new CaffeineCachePlugin())
                .registerRowMapper(new UserProfileMapper())
                .configure(SqlStatements.class, config -> config.setTemplateCache(new CaffeineCacheBuilder(
                        Caffeine.newBuilder()
                                .maximumSize(512L)
                                .expireAfterAccess(15L, TimeUnit.MINUTES)
                )));
    }

    /// Provides a singleton [HikariDataSource] for H2 and MySQL storage types.
    ///
    /// H2 is opened with `MODE=MySQL;DB_CLOSE_DELAY=-1` so the same SQL dialect is
    /// shared with the MySQL backend. This provider is not called when the storage
    /// type is [StorageType#JSON].
    ///
    /// @param primaryConfig holder for the primary plugin configuration
    /// @param dataDirectory the plugin data directory; used to resolve the H2 file path
    /// @return a started HikariCP connection pool
    /// @throws IllegalStateException if an unexpected SQL storage type is encountered
    @Provides
    @Singleton
    private HikariDataSource dataSource(
            final ConfigurationHolder<PrimaryConfiguration> primaryConfig,
            final @DataDirectory Path dataDirectory
    ) {
        final PrimaryConfiguration.StorageConfig storageConfig = primaryConfig.get().storage();
        final HikariConfig config = new HikariConfig();
        config.setPoolName("YourPlugin"); // TODO: change the pool name
        config.setMaximumPoolSize(storageConfig.maximumPoolSize());

        switch (storageConfig.type()) {
            case H2 -> {
                final Path dbFile = dataDirectory.toAbsolutePath().resolve("database");
                config.setJdbcUrl("jdbc:h2:file:%s;MODE=MySQL;DB_CLOSE_DELAY=-1".formatted(dbFile));
                config.setDriverClassName("org.h2.Driver");
            }
            case MYSQL -> {
                config.setJdbcUrl("jdbc:mysql://%s:%d/%s?useSSL=false&autoReconnect=true&characterEncoding=utf8"
                        .formatted(
                                storageConfig.host(),
                                storageConfig.port(),
                                storageConfig.database()
                        )
                );
                config.setUsername(storageConfig.username());
                config.setPassword(storageConfig.password());
                config.setDriverClassName("com.mysql.cj.jdbc.Driver");
            }
            default -> throw new IllegalStateException("Unexpected SQL storage type: " + storageConfig.type());
        }

        return new HikariDataSource(config);
    }
}

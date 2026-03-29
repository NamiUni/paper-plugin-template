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
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.jdbi.v3.cache.caffeine.CaffeineCacheBuilder;
import org.jdbi.v3.cache.caffeine.CaffeineCachePlugin;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.statement.SqlStatements;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.jspecify.annotations.NullMarked;

@NullMarked
@SuppressWarnings("unused")
public final class StorageModule extends AbstractModule {

    public StorageModule() {
    }

    /// Provides the singleton [UserRepository].
    ///
    /// @param primaryConfig TODO
    /// @param dataDirectory TODO
    /// @param jdbi       the configured JDBI instance
    /// @param dataSource the connection pool; ownership is transferred to the repository
    /// @return the initialized repository
    @Provides
    @Singleton
    private UserRepository userRepository(
            final ConfigurationHolder<PrimaryConfiguration> primaryConfig,
            final @DataDirectory Path dataDirectory,
            final Provider<Jdbi> jdbi,
            final Provider<HikariDataSource> dataSource
    ) {
        final var storageConfig = primaryConfig.get().storage();

        return switch (storageConfig.type()) {
            case JSON -> new JsonUserRepository(dataDirectory);
            case H2, MYSQL -> new JdbiUserRepository(jdbi.get(), dataSource.get());
        };
    }

    /// Provides a singleton [Jdbi] instance with all required plugins installed.
    ///
    /// @param dataSource the HikariCP pool
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
                                .executor(Executors.newThreadPerTaskExecutor(
                                        Thread.ofVirtual().factory() // TODO
                                ))
                )));
    }

    /// Provides a singleton [HikariDataSource] for SQL storage types.
    ///
    /// H2 is opened with `MODE=MySQL;DB_CLOSE_DELAY=-1` to stay compatible with
    /// the same SQL dialect used by the MySQL backend. Not called when the
    /// storage type is [StorageType#JSON].
    ///
    /// @param primaryConfig TODO
    /// @return a started HikariCP connection pool
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

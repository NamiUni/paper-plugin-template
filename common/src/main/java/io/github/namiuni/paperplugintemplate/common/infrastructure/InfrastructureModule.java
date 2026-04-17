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
package io.github.namiuni.paperplugintemplate.common.infrastructure;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.TypeLiteral;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.github.namiuni.kotonoha.translatable.message.KotonohaMessage;
import io.github.namiuni.kotonoha.translatable.message.configuration.FormatTypes;
import io.github.namiuni.kotonoha.translatable.message.policy.argument.TranslationArgumentAdaptationPolicy;
import io.github.namiuni.kotonoha.translatable.message.policy.argument.tag.TagNameResolver;
import io.github.namiuni.kotonoha.translatable.message.utility.TranslationArgumentAdapter;
import io.github.namiuni.paperplugintemplate.api.PluginTemplate;
import io.github.namiuni.paperplugintemplate.common.Metadata;
import io.github.namiuni.paperplugintemplate.common.infrastructure.configuration.ConfigurationHolder;
import io.github.namiuni.paperplugintemplate.common.infrastructure.configuration.ConfigurationLoader;
import io.github.namiuni.paperplugintemplate.common.infrastructure.configuration.configurations.PrimaryConfiguration;
import io.github.namiuni.paperplugintemplate.common.infrastructure.persistence.StorageDialect;
import io.github.namiuni.paperplugintemplate.common.infrastructure.persistence.UserRecord;
import io.github.namiuni.paperplugintemplate.common.infrastructure.persistence.UserRepository;
import io.github.namiuni.paperplugintemplate.common.infrastructure.persistence.UserRepositoryProvider;
import io.github.namiuni.paperplugintemplate.common.infrastructure.translation.TranslatorHolder;
import io.github.namiuni.paperplugintemplate.common.infrastructure.translation.translations.MessageAssembly;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;
import java.util.function.Consumer;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import net.kyori.adventure.translation.Translator;
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
public final class InfrastructureModule extends AbstractModule {

    private final ComponentLogger logger;
    private final Path dataDirectory;

    public InfrastructureModule(final ComponentLogger logger, final Path dataDirectory) {
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Provides
    @Singleton
    @SuppressWarnings("unused")
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
    private StorageDialect storageDialect(final Provider<PrimaryConfiguration> primaryConfig) {
        return switch (primaryConfig.get().storage().type()) {
            case H2, MYSQL -> new StorageDialect.MySQL();
            case POSTGRESQL -> new StorageDialect.PostgreSQL();
            case JSON -> throw new IllegalArgumentException("StorageType.JSON has no SQL dialect");
        };
    }

    @Provides
    @Singleton
    @SuppressWarnings("unused")
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

    @Provides
    @Singleton
    @SuppressWarnings("unused")
    private HikariDataSource dataSource(
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
    private ConfigurationLoader<PrimaryConfiguration> primaryConfigLoader(
            final @DataDirectory Path dataDirectory,
            final ComponentLogger logger
    ) {
        return new ConfigurationLoader<>(
                PrimaryConfiguration.class,
                PrimaryConfiguration.DEFAULT,
                dataDirectory,
                logger
        );
    }

    @Provides
    @Singleton
    @SuppressWarnings("unused")
    private MessageAssembly messageAssembly() {
        final var argumentPolicy = TranslationArgumentAdaptationPolicy.miniMessage(
                TranslationArgumentAdapter.standard(),
                TagNameResolver.annotationOrParameterNameResolver()
        );
        final var config = FormatTypes.MINI_MESSAGE.withArgumentPolicy(argumentPolicy);
        return KotonohaMessage.createProxy(MessageAssembly.class, config);
    }

    @Override
    protected void configure() {
        this.bind(ComponentLogger.class).toInstance(this.logger);
        this.bind(Path.class).annotatedWith(DataDirectory.class).toInstance(this.dataDirectory);

        this.bind(PrimaryConfiguration.class)
                .toProvider(new TypeLiteral<ConfigurationHolder<PrimaryConfiguration>>() { });
        this.bind(new TypeLiteral<Reloadable<PrimaryConfiguration>>() { })
                .to(new TypeLiteral<ConfigurationHolder<PrimaryConfiguration>>() { });

        this.bind(Translator.class).toProvider(new TypeLiteral<TranslatorHolder>() { });
        this.bind(new TypeLiteral<Reloadable<Translator>>() { })
                .to(new TypeLiteral<TranslatorHolder>() { });

        this.bind(UserRepository.class)
                .toProvider(UserRepositoryProvider.class)
                .asEagerSingleton();
    }
}

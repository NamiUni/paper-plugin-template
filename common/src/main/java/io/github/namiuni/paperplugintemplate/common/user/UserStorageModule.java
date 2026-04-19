package io.github.namiuni.paperplugintemplate.common.user;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.multibindings.Multibinder;
import io.github.namiuni.paperplugintemplate.common.infrastructure.configuration.configurations.PrimaryConfiguration;
import io.github.namiuni.paperplugintemplate.common.infrastructure.storage.JdbiConfigurer;
import io.github.namiuni.paperplugintemplate.common.user.json.JsonUserRepository;
import io.github.namiuni.paperplugintemplate.common.user.sql.JdbiUserRepository;
import io.github.namiuni.paperplugintemplate.common.user.sql.UserDao;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import org.jspecify.annotations.NullMarked;

@NullMarked
public final class UserStorageModule extends AbstractModule {

    @Override
    protected void configure() {
        Multibinder.newSetBinder(this.binder(), JdbiConfigurer.class)
                .addBinding()
                .toInstance((jdbi, dialect) -> jdbi.registerRowMapper(UserRecord.class, UserDao.rowMapper(dialect)));
    }

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
}

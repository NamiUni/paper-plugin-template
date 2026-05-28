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
package io.github.namiuni.paperplugintemplate.common.user;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;
import io.github.namiuni.paperplugintemplate.api.user.PluginTemplateUserService;
import io.github.namiuni.paperplugintemplate.common.infrastructure.DataDirectory;
import io.github.namiuni.paperplugintemplate.common.infrastructure.configuration.ConfigurationHolder;
import io.github.namiuni.paperplugintemplate.common.infrastructure.configuration.ConfigurationLoader;
import io.github.namiuni.paperplugintemplate.common.infrastructure.storage.StorageConfiguration;
import io.github.namiuni.paperplugintemplate.common.user.storage.JdbiUserRepository;
import io.github.namiuni.paperplugintemplate.common.user.storage.JsonUserRepository;
import io.github.namiuni.paperplugintemplate.common.user.storage.UserRepository;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import java.nio.file.Path;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.jspecify.annotations.NullMarked;
import org.spongepowered.configurate.serialize.TypeSerializerCollection;

@NullMarked
public final class UserModule extends AbstractModule {

    @Provides
    @Singleton
    @SuppressWarnings("unused")
    UserRepository userRepository(
            final ConfigurationHolder<StorageConfiguration> config,
            final Provider<JsonUserRepository> json,
            final Provider<JdbiUserRepository> jdbi
    ) {
        return switch (config.get().type()) {
            case JSON -> json.get();
            case H2, MYSQL, POSTGRESQL -> jdbi.get();
        };
    }

    @Provides
    @Singleton
    @SuppressWarnings("unused")
    ConfigurationLoader<UserConfiguration> configLoader(
            final @DataDirectory Path dataDirectory,
            final TypeSerializerCollection typeSerializers,
            final ComponentLogger logger
    ) {
        return new ConfigurationLoader<>(
                UserConfiguration.class,
                UserConfiguration.DEFAULT,
                dataDirectory,
                typeSerializers,
                logger
        );
    }

    @Override
    protected void configure() {
        this.bind(PluginTemplateUserService.class).to(UserServiceInternal.class).in(Scopes.SINGLETON);
        this.bind(new TypeLiteral<ConfigurationHolder<UserConfiguration>>() { }).asEagerSingleton();
        this.bind(UserConfiguration.class).toProvider(new TypeLiteral<ConfigurationHolder<UserConfiguration>>() { });
    }
}

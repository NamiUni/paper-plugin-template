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
package io.github.namiuni.paperplugintemplate.common.command;

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import io.github.namiuni.paperplugintemplate.common.infrastructure.DataDirectory;
import io.github.namiuni.paperplugintemplate.common.infrastructure.Reloadable;
import io.github.namiuni.paperplugintemplate.common.infrastructure.configuration.ConfigHolder;
import io.github.namiuni.paperplugintemplate.common.infrastructure.configuration.ConfigLoader;
import io.github.namiuni.paperplugintemplate.common.infrastructure.storage.StorageConfig;
import io.github.namiuni.paperplugintemplate.common.user.UserConfig;
import jakarta.inject.Singleton;
import java.nio.file.Path;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.jspecify.annotations.NullMarked;
import org.spongepowered.configurate.serialize.TypeSerializerCollection;

@NullMarked
public final class CommandModule extends AbstractModule {

    public CommandModule() {
    }

    @Provides
    @Singleton
    @SuppressWarnings("unused")
    ConfigLoader<CommandConfig> configLoader(
            final @DataDirectory Path dataDirectory,
            final TypeSerializerCollection typeSerializers,
            final ComponentLogger logger
    ) {
        return new ConfigLoader<>(
                CommandConfig.class,
                CommandConfig.DEFAULT,
                dataDirectory,
                typeSerializers,
                logger
        );
    }

    @Override
    protected void configure() {
        this.bind(new TypeLiteral<ConfigHolder<CommandConfig>>() { }).asEagerSingleton();
        this.bind(CommandConfig.class).toProvider(new TypeLiteral<ConfigHolder<CommandConfig>>() { });

        final Multibinder<Reloadable<?>> binder = Multibinder.newSetBinder(this.binder(), Key.get(new TypeLiteral<>() { }));
        binder.addBinding().to(Key.get(new TypeLiteral<ConfigHolder<CommandConfig>>() { }));
        binder.addBinding().to(Key.get(new TypeLiteral<ConfigHolder<StorageConfig>>() { }));
        binder.addBinding().to(Key.get(new TypeLiteral<ConfigHolder<UserConfig>>() { }));
    }
}

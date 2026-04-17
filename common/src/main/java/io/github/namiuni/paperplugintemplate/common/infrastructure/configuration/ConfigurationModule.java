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
package io.github.namiuni.paperplugintemplate.common.infrastructure.configuration;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.TypeLiteral;
import io.github.namiuni.paperplugintemplate.common.infrastructure.DataDirectory;
import io.github.namiuni.paperplugintemplate.common.infrastructure.Reloadable;
import io.github.namiuni.paperplugintemplate.common.infrastructure.configuration.configurations.PrimaryConfiguration;
import jakarta.inject.Singleton;
import java.nio.file.Path;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jspecify.annotations.NullMarked;

@NullMarked
public final class ConfigurationModule extends AbstractModule {

    @Override
    protected void configure() {
        this.bind(PrimaryConfiguration.class)
                .toProvider(new TypeLiteral<ConfigurationHolder<PrimaryConfiguration>>() { });
        this.bind(new TypeLiteral<Reloadable<PrimaryConfiguration>>() { })
                .to(new TypeLiteral<ConfigurationHolder<PrimaryConfiguration>>() { });
    }

    @Provides
    @Singleton
    @SuppressWarnings("unused")
    private ConfigurationLoader<PrimaryConfiguration> primaryConfigLoader(
            final @DataDirectory Path dataDirectory,
            final MiniMessage miniMessage,
            final ComponentLogger logger
    ) {
        return new ConfigurationLoader<>(
                PrimaryConfiguration.class,
                PrimaryConfiguration.DEFAULT,
                dataDirectory,
                miniMessage,
                logger
        );
    }
}

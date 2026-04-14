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

import io.github.namiuni.paperplugintemplate.common.infrastructure.Reloadable;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import java.util.concurrent.atomic.AtomicReference;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.jspecify.annotations.NullMarked;

@Singleton
@NullMarked
public final class ConfigurationHolder<T extends Record> implements Provider<T>, Reloadable<T> {

    private final ConfigurationLoader<T> configLoader;
    private final AtomicReference<T> config;
    private final ComponentLogger logger;

    @Inject
    private ConfigurationHolder(
            final ConfigurationLoader<T> configLoader,
            final ComponentLogger logger
    ) {
        this.configLoader = configLoader;
        this.logger = logger;

        this.logger.info("Loading configuration: {}...", configLoader.configName());
        this.config = new AtomicReference<>(configLoader.loadConfiguration());
        this.logger.info("Configuration loaded: {}", configLoader.configName());
    }

    public T reload() throws UncheckedConfigurateException {
        this.logger.info("Reloading configuration: {}...", this.configLoader.configName());
        final T loaded = this.configLoader.loadConfiguration();
        this.config.set(loaded);
        this.logger.info("Configuration reloaded: {}", this.configLoader.configName());
        return loaded;
    }

    @Override
    public T get() {
        return this.config.get();
    }
}

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
package io.github.namiuni.paperplugintemplate.common.configuration;

import jakarta.inject.Inject;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.jspecify.annotations.NullMarked;
import org.spongepowered.configurate.ConfigurateException;

/// Thread-safe holder for a live configuration instance.
///
/// Wraps a [ConfigurationLoader] and stores the most recently loaded
/// configuration in an [AtomicReference], allowing the value to be atomically
/// replaced during a hot-reload without blocking readers.
///
/// Implements [Supplier] so that consumers can obtain the current configuration
/// value without a direct dependency on this holder class.
///
/// @param <T> the configuration record type managed by this holder
@NullMarked
public final class ConfigurationHolder<T extends Record> implements Supplier<T> {

    private final ConfigurationLoader<T> configLoader;
    private final AtomicReference<T> config;
    private final ComponentLogger logger;

    /// Constructs a new holder by performing an initial load from disk.
    ///
    /// @param configLoader the loader used for both initial and subsequent loads
    /// @param logger       the component-aware logger
    /// @throws ConfigurateException if the initial configuration load fails
    @Inject
    private ConfigurationHolder(
            final ConfigurationLoader<T> configLoader,
            final ComponentLogger logger
    ) throws ConfigurateException {
        this.configLoader = configLoader;
        this.logger = logger;

        this.logger.info("Loading configuration: {}...", configLoader.configName());
        this.config = new AtomicReference<>(configLoader.loadConfiguration());
        this.logger.info("Configuration loaded: {}", configLoader.configName());
    }

    /// Reloads the configuration from disk and atomically updates the stored value.
    ///
    /// @return the freshly loaded configuration instance
    /// @throws ConfigurateException if the configuration file cannot be read or parsed
    public T reload() throws ConfigurateException {
        this.logger.info("Reloading configuration: {}...", this.configLoader.configName());
        final T loaded = this.configLoader.loadConfiguration();
        this.config.set(loaded);
        this.logger.info("Configuration reloaded: {}", this.configLoader.configName());
        return loaded;
    }

    /// Returns the currently active configuration instance.
    ///
    /// @return the current configuration, never `null`
    @Override
    public T get() {
        return this.config.get();
    }
}

/*
 * PluginTemplate
 *
 * Copyright (c) 2025. Namiu/Unitarou
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
package com.github.namiuni.plugintemplate.configuration;

import com.github.namiuni.plugintemplate.DataDirectory;
import com.github.namiuni.plugintemplate.exception.PluginConfigurationException;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.kyori.adventure.serializer.configurate4.ConfigurateComponentSerializer;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.jspecify.annotations.NullMarked;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.hocon.HoconConfigurationLoader;
import org.spongepowered.configurate.loader.ConfigurationLoader;

import java.nio.file.Path;

@Singleton
@NullMarked
public final class ConfigurationManager {

    private static final String PRIMARY_CONFIG_FILE_NAME = "config.conf";

    private final Path dataDirectory;
    private final ComponentLogger logger;

    private @MonotonicNonNull PrimaryConfig primaryConfig = null;

    @Inject
    private ConfigurationManager(
            final @DataDirectory Path dataDirectory,
            final ComponentLogger logger
    ) {
        this.dataDirectory = dataDirectory;
        this.logger = logger;
    }

    public void loadConfigurations() throws PluginConfigurationException {
        this.logger.info("Loading configurations...");
        this.primaryConfig = this.load(PrimaryConfig.class, PRIMARY_CONFIG_FILE_NAME);
        this.logger.info("Successfully loaded configurations: {}", PRIMARY_CONFIG_FILE_NAME);
    }

    public PrimaryConfig primary() {
        return this.primaryConfig;
    }

    public ConfigurationLoader<CommentedConfigurationNode> configurationLoader(final Path file) {
        return HoconConfigurationLoader.builder()
                .prettyPrinting(true)
                .defaultOptions(options -> {
                    final var kyoriSerializer = ConfigurateComponentSerializer.configurate();
                    return options
                            .shouldCopyDefaults(true)
                            .serializers(serializerBuilder -> serializerBuilder
                                    .registerAll(kyoriSerializer.serializers()));
                })
                .path(file)
                .build();
    }

    public <T> T load(final Class<T> clazz, final String fileName) throws PluginConfigurationException {
        final Path file = this.dataDirectory.resolve(fileName);
        final var loader = this.configurationLoader(file);

        final CommentedConfigurationNode node;
        final T config;
        try {
            node = loader.load();
            config = node.get(clazz);
            if (config == null) {
                throw new ConfigurateException(node, "Failed to deserialize " + clazz.getName() + " from node");
            }

            node.set(clazz, config);
            loader.save(node);
        } catch (final ConfigurateException exception) {
            throw new PluginConfigurationException("Failed to load configuration", exception);
        }

        return config;
    }
}

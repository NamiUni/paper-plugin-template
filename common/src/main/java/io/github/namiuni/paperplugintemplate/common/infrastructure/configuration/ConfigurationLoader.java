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

import io.github.namiuni.paperplugintemplate.common.infrastructure.DataDirectory;
import io.github.namiuni.paperplugintemplate.common.infrastructure.configuration.annotations.ConfigHeader;
import io.github.namiuni.paperplugintemplate.common.infrastructure.configuration.annotations.ConfigName;
import java.nio.file.Path;
import net.kyori.adventure.serializer.configurate4.ConfigurateComponentSerializer;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jspecify.annotations.NullMarked;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.hocon.HoconConfigurationLoader;

@NullMarked
public final class ConfigurationLoader<T extends Record> {

    private final Class<T> configClass;
    private final T defaultConfig;
    private final String configName;
    private final ComponentLogger logger;

    private final org.spongepowered.configurate.loader.ConfigurationLoader<CommentedConfigurationNode> configLoader;

    ConfigurationLoader(
            final Class<T> configClass,
            final T defaultConfig,
            final @DataDirectory Path dataDirectory,
            final MiniMessage miniMessage,
            final ComponentLogger logger
    ) {
        this.configClass = configClass;
        this.defaultConfig = defaultConfig;
        this.logger = logger;

        // Config path
        this.configName = configClass.getAnnotation(ConfigName.class).value();
        final Path configPath = dataDirectory.resolve(this.configName);

        // Config header
        final ConfigHeader headerAnnotation = configClass.getAnnotation(ConfigHeader.class);
        final String configHeader = headerAnnotation.value();

        final var kyoriSerializer = ConfigurateComponentSerializer.builder()
                .scalarSerializer(miniMessage)
                .build()
                .serializers();

        this.configLoader = HoconConfigurationLoader.builder()
                .prettyPrinting(true)
                .defaultOptions(options -> options
                        .shouldCopyDefaults(true)
                        .header(configHeader)
                        .serializers(builder -> builder
                                .registerAll(kyoriSerializer)
                        )
                )
                .path(configPath)
                .build();
    }

    T loadConfiguration() throws UncheckedConfigurateException {
        this.logger.debug("[{}] Reading {} from disk...", ConfigurationLoader.class.getSimpleName(), this.configName);
        final ConfigurationNode node;
        try {
            node = this.configLoader.load();
            final T config = node.get(this.configClass, this.defaultConfig);
            this.logger.debug("[{}] Loaded configuration: {}", ConfigurationHolder.class.getSimpleName(), config);
            this.configLoader.save(node);
            this.logger.debug("[{}] Wrote defaults back to {} (shouldCopyDefaults).", ConfigurationLoader.class.getSimpleName(), this.configName);
            return config;
        } catch (final ConfigurateException exception) {
            throw new UncheckedConfigurateException(exception);
        }
    }

    String configName() {
        return this.configName;
    }
}

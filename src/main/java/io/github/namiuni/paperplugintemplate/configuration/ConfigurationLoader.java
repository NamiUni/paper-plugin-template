/*
 * paper-plugin-template
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
package io.github.namiuni.paperplugintemplate.configuration;

import io.github.namiuni.paperplugintemplate.configuration.annotations.ConfigHeader;
import io.github.namiuni.paperplugintemplate.configuration.annotations.ConfigName;
import io.github.namiuni.paperplugintemplate.guice.DataDirectory;
import java.nio.file.Path;
import net.kyori.adventure.serializer.configurate4.ConfigurateComponentSerializer;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jspecify.annotations.NullMarked;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

@NullMarked
public final class ConfigurationLoader<T extends Record> {

    private final Class<T> configClass;
    private final T defaultConfig;

    private final org.spongepowered.configurate.loader.ConfigurationLoader<CommentedConfigurationNode> configLoader;

    public ConfigurationLoader(
            final Class<T> configClass,
            final T defaultConfig,
            final @DataDirectory Path dataDirectory
    ) {
        this.configClass = configClass;
        this.defaultConfig = defaultConfig;

        // Config path
        final String configName = configClass.getAnnotation(ConfigName.class).value();
        final Path configPath = dataDirectory.resolve(configName);

        // Config header
        final ConfigHeader headerAnnotation = configClass.getAnnotation(ConfigHeader.class);
        final String configHeader = headerAnnotation.value();

        final var kyoriSerializer = ConfigurateComponentSerializer.builder()
                .scalarSerializer(MiniMessage.miniMessage())
                .build()
                .serializers();

        this.configLoader = YamlConfigurationLoader.builder()
                .nodeStyle(NodeStyle.BLOCK)
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

    T loadConfiguration() throws ConfigurateException {
        final ConfigurationNode node = this.configLoader.load();
        final T config = node.get(this.configClass, this.defaultConfig);
        this.configLoader.save(node);
        return config;
    }
}

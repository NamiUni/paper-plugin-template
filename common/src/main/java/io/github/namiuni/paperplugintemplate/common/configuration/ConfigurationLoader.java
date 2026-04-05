/*
 * PaperPluginTemplate
 *
 * Copyright (c) 2026. Namiu (ãã«ããã)
 *                     Contributors
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

import io.github.namiuni.paperplugintemplate.common.DataDirectory;
import io.github.namiuni.paperplugintemplate.common.configuration.annotations.ConfigHeader;
import io.github.namiuni.paperplugintemplate.common.configuration.annotations.ConfigName;
import java.nio.file.Path;
import net.kyori.adventure.serializer.configurate4.ConfigurateComponentSerializer;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jspecify.annotations.NullMarked;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.hocon.HoconConfigurationLoader;

/// Loads and saves a typed configuration record backed by a YAML file on disk.
///
/// The file name and optional header comment are taken from the [ConfigName]
/// and [ConfigHeader] annotations on the configuration class. Missing keys are
/// filled in from the supplied `defaultConfig` and the file is immediately
/// re-saved so that new options become visible to the server operator.
///
/// Adventure [net.kyori.adventure.text.Component] values are supported as
/// configuration field types via the Configurate Adventure serializer.
///
/// ## Thread safety
///
/// This class is **not** thread-safe. Concurrent calls to
/// [#loadConfiguration()] produce undefined behavior. [ConfigurationHolder]
/// ensures that load operations are serialized; callers that bypass the holder
/// must provide their own synchronization.
///
/// @param <T> the configuration record type; must extend [Record]
@NullMarked
public final class ConfigurationLoader<T extends Record> {

    private final Class<T> configClass;
    private final T defaultConfig;

    private final org.spongepowered.configurate.loader.ConfigurationLoader<CommentedConfigurationNode> configLoader;

    /// Constructs a new loader for the given configuration class.
    ///
    /// The [ConfigName] annotation on `configClass` determines the file name
    /// relative to `dataDirectory`. The [ConfigHeader] annotation, if present,
    /// provides a comment written at the top of the file.
    ///
    /// @param configClass   the configuration record class; must be annotated
    ///                      with [ConfigName] and [ConfigHeader]
    /// @param defaultConfig the fallback instance used when a key is absent
    ///                      from the file
    /// @param dataDirectory the plugin data directory where the file will be
    ///                      stored
    /// @throws NullPointerException if `configClass` is not annotated with
    ///         [ConfigName] or [ConfigHeader]
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

    /// Loads the configuration from disk, populates missing keys with
    /// defaults, and saves the result back to the file.
    ///
    /// @return the deserialized configuration instance
    /// @throws ConfigurateException if the file cannot be read, parsed, or written
    T loadConfiguration() throws ConfigurateException {
        final ConfigurationNode node = this.configLoader.load();
        final T config = node.get(this.configClass, this.defaultConfig);
        this.configLoader.save(node);
        return config;
    }
}

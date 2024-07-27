/*
 * plugin-template
 *
 * Copyright (c) 2024. Namiu (Unitarou)
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

package com.github.namiuni.plugintemplate.config;

import com.github.namiuni.plugintemplate.DataDirectory;
import com.github.namiuni.plugintemplate.config.serialisation.LocaleSerializer;
import com.github.namiuni.plugintemplate.util.FileUtil;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.kyori.adventure.serializer.configurate4.ConfigurateComponentSerializer;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.hocon.HoconConfigurationLoader;
import org.spongepowered.configurate.loader.ConfigurationLoader;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;

@Singleton
@DefaultQualifier(NonNull.class)
public final class ConfigManager {

    private final Path dataDirectory;
    private final ComponentLogger logger;
    private final LocaleSerializer localeSerializer;

    private @MonotonicNonNull PrimaryConfig primaryConfig = null;

    private static final String PRIMARY_CONFIG_FILE_NAME = "config.conf";

    @Inject
    public ConfigManager(
            final @DataDirectory Path dataDirectory,
            final ComponentLogger logger,
            final LocaleSerializer localeSerializer
    ) {
        this.dataDirectory = dataDirectory;
        this.logger = logger;
        this.localeSerializer = localeSerializer;
    }

    public void reloadPrimaryConfig() {
        this.logger.info("Reloading configuration...");
        final @Nullable PrimaryConfig load = this.load(PrimaryConfig.class, PRIMARY_CONFIG_FILE_NAME);
        if (load != null) {
            this.primaryConfig = load;
        } else {
            this.logger.error("Failed to reload primary config, see above for further details");
        }
    }

    public PrimaryConfig primaryConfig() {
        if (this.primaryConfig != null) {
            return this.primaryConfig;
        }

        this.logger.info("Loading configuration...");
        final @Nullable PrimaryConfig load = this.load(PrimaryConfig.class, PRIMARY_CONFIG_FILE_NAME);
        if (load == null) {
            throw new IllegalStateException("Failed to initialize primary config, see above for further details");
        }
        this.primaryConfig = load;
        return this.primaryConfig;
    }

    public ConfigurationLoader<CommentedConfigurationNode> configurationLoader(final Path file) {
        return HoconConfigurationLoader.builder()
                .prettyPrinting(true)
                .defaultOptions(options -> {
                    final var kyoriSerializer = ConfigurateComponentSerializer.configurate();
                    return options.shouldCopyDefaults(true).serializers(serializerBuilder -> serializerBuilder
                            .registerAll(kyoriSerializer.serializers())
                            .register(Locale.class, this.localeSerializer)
                    );
                })
                .path(file)
                .build();
    }

    public <T> @Nullable T load(final Class<T> clazz, final String fileName) {
        final Path file = this.dataDirectory.resolve(fileName);
        try {
            FileUtil.createDirectoriesIfNotExists(this.dataDirectory);
        } catch (final IOException exception) {
            this.logger.error("Failed to create directories: {}", this.dataDirectory, exception);
            return null;
        }

        final var loader = this.configurationLoader(file);
        try {
            final var node = loader.load();
            final @Nullable T config = node.get(clazz);
            if (config != null) {
                node.set(clazz, config);
                loader.save(node);
                return config;
            } else {
                throw new ConfigurateException(node, "Failed to deserialize " + clazz.getName() + " from node");
            }

        } catch (final ConfigurateException exception) {
            this.logger.error("Failed to load config: {}", file, exception);
            return null;
        }
    }
}

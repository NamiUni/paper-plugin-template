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

package com.github.namiuni.plugintemplate.message;

import com.github.namiuni.plugintemplate.DataDirectory;
import com.github.namiuni.plugintemplate.config.ConfigManager;
import com.github.namiuni.plugintemplate.util.FileUtil;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.translation.TranslationRegistry;
import net.kyori.adventure.translation.Translator;
import net.kyori.adventure.util.UTF8ResourceBundleControl;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Singleton
@DefaultQualifier(NonNull.class)
public final class TranslationManager {

    private static final List<Locale> INCLUDE_LOCALES = List.of(Locale.US, Locale.JAPAN);

    private final Set<Locale> installedLocales = ConcurrentHashMap.newKeySet();
    private @MonotonicNonNull TranslationRegistry registry;

    private final Path localeDirectory;
    private final ComponentLogger logger;
    private final ConfigManager configManager;

    @Inject
    public TranslationManager(
            final @DataDirectory Path dataDirectory,
            final ComponentLogger logger,
            final ConfigManager configManager
    ) {
        this.localeDirectory = dataDirectory.resolve("locale");
        this.logger = logger;
        this.configManager = configManager;

        this.reloadTranslations();
    }

    public @Nullable MessageFormat translate(final String key, final Locale locale) {
        return this.registry.translate(key, locale);
    }

    public void reloadTranslations() {
        if (this.registry != null) {
            GlobalTranslator.translator().removeSource(this.registry);
            this.installedLocales.clear();
        }

        this.registry = TranslationRegistry.create(Key.key("paperpluginexample", "messages"));
        this.registry.defaultLocale(configManager.primaryConfig().defaultLocale());
        this.logger.info("Default Locale: {}", configManager.primaryConfig().defaultLocale());

        loadFromLocaleDirectory();
        loadFromResourceBundle();

        GlobalTranslator.translator().addSource(this.registry);
        final var formated = this.installedLocales.stream()
                .map(Locale::toString)
                .collect(Collectors.joining(", "));
        this.logger.info("Loaded {} locales: [{}]", this.installedLocales.size(), formated);
    }

    public void loadFromLocaleDirectory() {
        try {
            FileUtil.createDirectoriesIfNotExists(this.localeDirectory);
        } catch (IOException exception) {
            this.logger.error("Failed to create directories for '{}'", this.localeDirectory, exception);
            return;
        }

        try {
            try (final var stream = Files.list(this.localeDirectory)) {
                stream.filter(Files::isRegularFile)
                        .filter(path -> path.toString().endsWith(".properties"))
                        .map(translationFile -> {
                            try {
                                return this.loadTranslationFile(translationFile);
                            } catch (final IOException exception) {
                                this.logger.warn("Error loading locale file: {}", translationFile.getFileName(), exception);
                            }
                            return null;
                        })
                        .filter(Objects::nonNull)
                        .forEach(entry -> this.registry.registerAll(entry.getKey(), entry.getValue(), false));
            }
        } catch (final IOException exception) {
            this.logger.error("Failed to load locales", exception);
        }
    }

    private void loadFromResourceBundle() {
        INCLUDE_LOCALES.forEach(locale -> {
            final var bundle = ResourceBundle.getBundle("locale/messages", locale, UTF8ResourceBundleControl.get());
            this.registry.registerAll(locale, bundle, false);
            this.installedLocales.add(locale);
        });
    }

    private Map.Entry<Locale, ResourceBundle> loadTranslationFile(final Path translationFile) throws IOException {
        final var fileName = translationFile.getFileName().toString();
        final var localeString = fileName
                .substring("messages_".length())
                .replace(".properties", "");
        final @Nullable Locale locale = this.parseLocale(localeString);

        if (locale == null) {
            throw new IllegalStateException("Unknown locale '" + localeString + "' - unable to register.");
        }

        PropertyResourceBundle bundle;
        try (BufferedReader reader = Files.newBufferedReader(translationFile, StandardCharsets.UTF_8)) {
            bundle = new PropertyResourceBundle(reader);
        }

        this.registry.registerAll(locale, bundle, false);
        this.installedLocales.add(locale);
        return Map.entry(locale, bundle);
    }

    private @Nullable Locale parseLocale(final @Nullable String locale) {
        return locale == null
                ? null
                : Translator.parseLocale(locale);
    }
}

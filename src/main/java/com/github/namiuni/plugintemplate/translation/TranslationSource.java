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
package com.github.namiuni.plugintemplate.translation;

import com.github.namiuni.plugintemplate.DataDirectory;
import com.github.namiuni.plugintemplate.util.MoreFiles;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import net.kyori.adventure.translation.Translator;
import net.kyori.adventure.util.UTF8ResourceBundleControl;
import net.kyori.moonshine.message.IMessageSource;
import org.jspecify.annotations.NullMarked;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// Based on LuckPerms design: https://github.com/LuckPerms/LuckPerms/blob/master/common/src/main/java/me/lucko/luckperms/common/locale/TranslationManager.java
@Singleton
@NullMarked
public final class TranslationSource implements IMessageSource<Audience, String> {

    private static final Locale FALLBACK_LOCALE = Locale.US;

    private final Path customTranslationsDirectory;
    private final Path repositoryTranslationsDirectory;
    private final ComponentLogger logger;
    private final TranslationRepository repository;

    private final Map<String, Translation> translations;
    private final Set<Locale> installedLocales;

    @Inject
    private TranslationSource(
            final @DataDirectory Path dataDirectory,
            final ComponentLogger logger,
            final TranslationRepository repository
    ) {
        final Path translationsDirectory = dataDirectory.resolve("translations");
        this.customTranslationsDirectory = translationsDirectory.resolve("custom");
        this.repositoryTranslationsDirectory = translationsDirectory.resolve("repository");
        this.logger = logger;
        this.repository = repository;

        this.translations = new ConcurrentHashMap<>();
        this.installedLocales = new HashSet<>();
    }

    @Override
    public String messageOf(final Audience receiver, final String messageKey) {
        final Translation translation = this.translations.get(messageKey);
        final Locale locale = receiver.getOrDefault(Identity.LOCALE, Locale.getDefault());
        return translation.message(locale);
    }

    public void loadTranslations() {
        try {
            MoreFiles.createDirectoriesIfNotExists(this.customTranslationsDirectory);
        } catch (final IOException exception) {
            throw new UncheckedIOException("Unable to create plugin directory", exception);
        }

        if (!this.translations.isEmpty()) {
            this.translations.clear();
        }

        this.logger.info("Loading translations...");
        this.repository.copyResourcesToDataDirectory();
        this.loadFromFileSystem(this.customTranslationsDirectory);
        this.loadFromFileSystem(this.repositoryTranslationsDirectory);
        this.loadFromResourceBundle();

        this.logger.info("Successfully loaded {} translations: [{}]", this.installedLocales.size(), this.installedLocales.stream().map(Locale::getDisplayName).collect(Collectors.joining(", ")));
    }

    private void loadFromFileSystem(final Path path) {
        try (final Stream<Path> pathStream = Files.list(path)) {
            pathStream
                    .filter(Files::isRegularFile)
                    .forEach(this::loadTranslationFile);
        } catch (final IOException exception) {
            throw new UncheckedIOException("Unable to load translations", exception);
        }
    }

    private void loadTranslationFile(final Path file) {
        final String localeString = extractLocaleString(file);
        final Locale locale = Translator.parseLocale(localeString);
        if (locale == null) {
            throw new IllegalStateException("Tried to load unknown locale %s: %s".formatted(localeString, file));
        }

        try (final BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            final ResourceBundle bundle = new PropertyResourceBundle(reader);
            this.registerTranslations(locale, bundle);
            this.installedLocales.add(locale);
        } catch (final IOException exception) {
            throw new UncheckedIOException("Unable to load translation file: %s".formatted(file.getFileName()), exception);
        }
    }

    private void loadFromResourceBundle() {
        final ResourceBundle bundle = ResourceBundle.getBundle("translations/messages", FALLBACK_LOCALE, UTF8ResourceBundleControl.get());
        this.registerTranslations(FALLBACK_LOCALE, bundle);
        this.installedLocales.add(bundle.getLocale());
    }

    private void registerTranslations(final Locale locale, final ResourceBundle bundle) {
        for (final String key : bundle.keySet()) {
            this.registerTranslation(key, locale, bundle.getString(key));
        }
    }

    private void registerTranslation(final String key, final Locale locale, final String message) {
        this.translations
                .computeIfAbsent(key, Translation::new)
                .register(locale, message);
    }

    private static String extractLocaleString(final Path localeFile) {
        final String fileName = localeFile.getFileName().toString();
        return fileName.substring("messages_".length()).replace(".properties", "");
    }

    private static final class Translation {

        private final Map<Locale, String> messages;

        Translation(final String key) {
            this.messages = new ConcurrentHashMap<>();
        }

        private void register(final Locale locale, final String message) {
            this.messages.putIfAbsent(locale, message);
        }

        private String message(final Locale locale) {
            return this.messages.getOrDefault(locale, this.messages.get(TranslationSource.FALLBACK_LOCALE));
        }
    }
}

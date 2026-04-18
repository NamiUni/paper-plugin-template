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
package io.github.namiuni.paperplugintemplate.common.infrastructure.translation;

import io.github.namiuni.paperplugintemplate.common.Metadata;
import io.github.namiuni.paperplugintemplate.common.infrastructure.DataDirectory;
import io.github.namiuni.paperplugintemplate.common.infrastructure.PluginResource;
import jakarta.inject.Inject;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.translation.MiniMessageTranslationStore;
import net.kyori.adventure.translation.Translator;
import org.intellij.lang.annotations.Subst;
import org.jspecify.annotations.NullMarked;

@NullMarked
final class TranslatorLoader {

    private static final String BASE_NAME = "messages";
    private static final String FILE_SUFFIX = ".properties";

    // ROOT locale is always sourced from the JAR bundle and never loaded from disk.
    // It is exported as this filename so that users can customize the English text
    // without accidentally replacing the ROOT fallback.
    private static final String ROOT_EXPORT_NAME = BASE_NAME + "_en_US" + FILE_SUFFIX;

    private final ComponentLogger logger;
    private final MiniMessage miniMessage;
    private final Path translationDir;
    private final Path pluginResource;
    private final Key translationKey;

    @Inject
    TranslatorLoader(
            final ComponentLogger logger,
            final MiniMessage miniMessage,
            final @DataDirectory Path dataDirectory,
            final @PluginResource Path pluginResource,
            final Metadata metadata
    ) {
        this.logger = logger;
        this.miniMessage = miniMessage;
        this.translationDir = dataDirectory.resolve("translations");
        this.pluginResource = pluginResource;

        try {
            Files.createDirectories(this.translationDir);
        } catch (final IOException exception) {
            throw new UncheckedIOException(exception);
        }

        final @Subst("namespace") String namespace = metadata.namespace();
        this.translationKey = Key.key(namespace, BASE_NAME);
    }

    Translator loadTranslator() {
        final MiniMessageTranslationStore store = MiniMessageTranslationStore.create(this.translationKey, this.miniMessage);
        store.defaultLocale(Locale.ROOT);

        final Set<Locale> diskLocales = this.registerDiskTranslations(store);
        this.registerJarTranslations(store, diskLocales);

        return store;
    }

    private Set<Locale> registerDiskTranslations(final MiniMessageTranslationStore store) {
        final Set<Locale> registered = new HashSet<>();
        try (final Stream<Path> files = Files.list(this.translationDir)) {
            files
                    .filter(Files::isRegularFile)
                    .filter(path -> isTranslationFile(path.getFileName().toString()))
                    .forEach(path -> {
                        final String fileName = path.getFileName().toString();
                        final Locale locale = parseLocale(fileName);

                        // ROOT locale is reserved for the JAR bundle. A disk file that
                        // resolves to ROOT (e.g. a hand-placed messages.properties) would
                        // corrupt the fallback chain, so we skip it with a clear warning.
                        if (locale == Locale.ROOT) {
                            this.logger.warn(
                                    "Skipped '{}': ROOT locale must not be overridden from disk. " +
                                    "Place your English customisations in '{}' instead.",
                                    fileName, ROOT_EXPORT_NAME
                            );
                            return;
                        }

                        store.registerAll(locale, path, false);
                        registered.add(locale);
                        this.logger.debug("[{}] Registered from disk: {} ({})", TranslatorLoader.class.getSimpleName(), fileName, locale);
                    });
        } catch (final IOException exception) {
            throw new UncheckedIOException(exception);
        }
        if (!registered.isEmpty()) {
            this.logger.info("Loaded {} translation file(s) from disk: {}.", registered.size(), registered);
        }
        return registered;
    }

    private void registerJarTranslations(final MiniMessageTranslationStore store, final Set<Locale> diskLocales) {
        try (final FileSystem jar = FileSystems.newFileSystem(this.pluginResource, TranslatorLoader.class.getClassLoader())) {
            final Path root = jar.getRootDirectories().iterator().next();
            try (final Stream<Path> paths = Files.walk(root)) {
                paths
                        .filter(Files::isRegularFile)
                        .filter(path -> isTranslationFile(path.getFileName().toString()))
                        .forEach(path -> this.processJarEntry(store, diskLocales, path));
            }
        } catch (final IOException exception) {
            this.logger.warn("Could not scan JAR for translations. Skipping resource export.", exception);
        }
    }

    private void processJarEntry(
            final MiniMessageTranslationStore store,
            final Set<Locale> diskLocales,
            final Path jarPath
    ) {
        final String fileName = jarPath.getFileName().toString();
        final Locale locale = parseLocale(fileName);

        // ROOT locale is always registered from the JAR, regardless of what is on disk,
        // to guarantee the fallback is never broken by user edits.
        // Non-ROOT locales defer to the disk override when present.
        if (locale == Locale.ROOT || !diskLocales.contains(locale)) {
            store.registerAll(locale, jarPath, false);
            this.logger.debug("[{}] Registered from JAR: {} ({})", TranslatorLoader.class.getSimpleName(), fileName, locale);
        }

        // ROOT locale is exported under a distinct, locale-tagged filename so that
        // users can customize it without it being picked up as the ROOT fallback on
        // the next reload. Non-ROOT files are exported under their original names.
        final String diskFileName = locale == Locale.ROOT ? ROOT_EXPORT_NAME : fileName;
        final Path diskTarget = this.translationDir.resolve(diskFileName);
        if (Files.notExists(diskTarget)) {
            this.exportDefault(jarPath, diskTarget, diskFileName);
        }
    }

    private void exportDefault(final Path source, final Path target, final String fileName) {
        try {
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            this.logger.debug("[{}] Exported default: {}", TranslatorLoader.class.getSimpleName(), fileName);
        } catch (final IOException exception) {
            this.logger.error("Failed to export default: {}", fileName, exception);
        }
    }

    private static boolean isTranslationFile(final String name) {
        return name.startsWith(BASE_NAME) && name.endsWith(FILE_SUFFIX);
    }

    private static Locale parseLocale(final String fileName) {
        if (Objects.equals(BASE_NAME + FILE_SUFFIX, fileName)) {
            return Locale.ROOT;
        }

        final String tag = fileName.substring(BASE_NAME.length() + 1, fileName.length() - FILE_SUFFIX.length());
        final Locale locale = Translator.parseLocale(tag);
        if (locale == null) {
            throw new IllegalArgumentException("Cannot parse locale from translation file name: " + fileName);
        }

        return locale;
    }
}

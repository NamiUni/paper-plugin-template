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

import io.github.namiuni.kotonoha.annotations.Key;
import io.github.namiuni.kotonoha.annotations.Message;
import io.github.namiuni.paperplugintemplate.common.Metadata;
import io.github.namiuni.paperplugintemplate.common.infrastructure.DataDirectory;
import io.github.namiuni.paperplugintemplate.common.infrastructure.translation.translations.MessageAssembly;
import jakarta.inject.Inject;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.translation.MiniMessageTranslationStore;
import net.kyori.adventure.translation.Translator;
import org.intellij.lang.annotations.Subst;
import org.jspecify.annotations.NullMarked;

@NullMarked
final class TranslatorLoader {

    private static final String FILE_PREFIX = "messages";
    private static final String FILE_SUFFIX = ".properties";

    private final ComponentLogger logger;
    private final MiniMessage miniMessage;
    private final Path translationDir;
    private final net.kyori.adventure.key.Key translationKey;

    @Inject
    private TranslatorLoader(
            final ComponentLogger logger,
            final MiniMessage miniMessage,
            final @DataDirectory Path dataDirectory,
            final Metadata metadata
    ) {
        this.logger = logger;
        this.miniMessage = miniMessage;

        this.translationDir = dataDirectory.resolve("translations");
        try {
            Files.createDirectories(this.translationDir);
        } catch (final IOException exception) {
            throw new UncheckedIOException(exception);
        }

        final @Subst("namespace") String namespace = metadata.namespace();
        this.translationKey = net.kyori.adventure.key.Key.key(namespace, "messages");
    }

    Translator loadTranslator() {
        this.logger.debug("[{}] Building translation store...", TranslatorLoader.class.getSimpleName());
        final var store = MiniMessageTranslationStore.create(this.translationKey, this.miniMessage);
        store.defaultLocale(Locale.ROOT);

        final Map<Locale, List<Translation.Message>> index = buildIndex();

        // 1. Register ROOT locale
        final List<Translation.Message> rootMessages = index.getOrDefault(Locale.ROOT, List.of());
        if (!rootMessages.isEmpty()) {
            final Map<String, String> rootMap = new LinkedHashMap<>(rootMessages.size());
            for (final Translation.Message msg : rootMessages) {
                rootMap.put(msg.key(), msg.content());
            }
            store.registerAll(Locale.ROOT, rootMap);
            this.logger.debug(
                    "[{}] Registered {} ROOT-locale messages from annotations.",
                    TranslatorLoader.class.getSimpleName(), rootMap.size()
            );
        }

        // 2. Register operator-provided translation files from disk
        final Set<Locale> diskLocales = this.loadDiskLocales(store);

        // 3. Fill annotation-defined locales absent on disk; write default files
        for (final Map.Entry<Locale, List<Translation.Message>> entry : index.entrySet()) {
            final Locale locale = entry.getKey();
            final List<Translation.Message> messages = entry.getValue();

            for (final Translation.Message msg : messages) {
                if (!store.contains(msg.key(), locale)) {
                    store.register(msg.key(), locale, msg.content());
                }
            }

            if (!diskLocales.contains(locale)) {
                writeTranslationFile(this.translationDir, new Translation(locale, messages));
                this.logger.debug(
                        "[{}] Generated default translation file for locale: {}.",
                        TranslatorLoader.class.getSimpleName(), locale
                );
            }
        }

        this.logger.debug("[{}] Translation store build complete.", TranslatorLoader.class.getSimpleName());
        return store;
    }

    private static Map<Locale, List<Translation.Message>> buildIndex() {
        final Map<Locale, List<Translation.Message>> index = new LinkedHashMap<>();

        Arrays.stream(MessageAssembly.class.getMethods())
                .filter(method -> method.isAnnotationPresent(Key.class))
                .sorted(Comparator.comparing(method -> method.getAnnotation(Key.class).value()))
                .forEach(method -> {
                    final String key = method.getAnnotation(Key.class).value();
                    for (final Message annotation : method.getAnnotationsByType(Message.class)) {
                        index.computeIfAbsent(annotation.locale().asLocale(), _ -> new ArrayList<>())
                                .add(new Translation.Message(key, annotation.content()));
                    }
                });

        return index;
    }

    private Set<Locale> loadDiskLocales(final MiniMessageTranslationStore store) {
        final var diskLocales = new java.util.HashSet<Locale>();
        try (Stream<Path> files = Files.list(this.translationDir)) {
            files.filter(Files::isRegularFile)
                    .filter(TranslatorLoader::isTranslationFile)
                    .forEach(file -> {
                        final Locale locale = parseLocale(file);
                        store.registerAll(locale, file, false);
                        diskLocales.add(locale);
                        this.logger.debug(
                                "[{}] Loaded translation file: {} (locale: {})",
                                TranslatorLoader.class.getSimpleName(), file.getFileName(), locale
                        );
                    });
        } catch (final IOException exception) {
            throw new UncheckedIOException(exception);
        }

        if (diskLocales.isEmpty()) {
            this.logger.debug(
                    "[{}] No operator-provided translation files found in {}.",
                    TranslatorLoader.class.getSimpleName(), this.translationDir
            );
        } else {
            this.logger.info("Loaded {} translation file(s) from disk: {}.", diskLocales.size(), diskLocales);
        }

        return diskLocales;
    }

    private static boolean isTranslationFile(final Path file) {
        final String name = file.getFileName().toString();
        return name.startsWith(FILE_PREFIX) && name.endsWith(FILE_SUFFIX);
    }

    private static Locale parseLocale(final Path file) {
        final String name = file.getFileName().toString();
        final String tag = name.substring(FILE_PREFIX.length() + 1, name.length() - FILE_SUFFIX.length());
        final Locale locale = Translator.parseLocale(tag);
        if (locale == null) {
            throw new IllegalArgumentException("Cannot parse locale from translation file name: " + name);
        }
        return locale;
    }

    private static void writeTranslationFile(final Path parentDir, final Translation translation) {
        final Locale locale = translation.locale() == Locale.ROOT ? Locale.US : translation.locale();
        if (locale == Locale.ROOT) {
            return;
        }
        final String fileName = FILE_PREFIX + "_" + locale + FILE_SUFFIX;
        try (BufferedWriter writer = Files.newBufferedWriter(parentDir.resolve(fileName))) {
            for (final Translation.Message entry : translation.messages()) {
                writer.write(entry.key());
                writer.write('=');
                writer.write(entry.content());
                writer.newLine();
            }
        } catch (final IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}

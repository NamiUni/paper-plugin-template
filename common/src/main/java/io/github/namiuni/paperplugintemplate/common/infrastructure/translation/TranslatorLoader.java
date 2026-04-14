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
import io.github.namiuni.paperplugintemplate.common.infrastructure.DataDirectory;
import io.github.namiuni.paperplugintemplate.common.infrastructure.translation.translations.MessageAssembly;
import jakarta.inject.Inject;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.translation.MiniMessageTranslationStore;
import net.kyori.adventure.translation.Translator;
import org.jspecify.annotations.NullMarked;

@NullMarked
final class TranslatorLoader {

    // JIS Z 9103 https://ja.wikipedia.org/wiki/JIS%E5%AE%89%E5%85%A8%E8%89%B2
    private static final TextColor RED = TextColor.color(0xFF4B00);
    private static final TextColor YELLOW = TextColor.color(0xF2E700);
    private static final TextColor GREEN = TextColor.color(0x00B06B);
    private static final TextColor BLUE = TextColor.color(0x1971FF);

    private static final MiniMessage MINI_MESSAGE = MiniMessage.builder()
            .tags(TagResolver.builder()
                    .resolver(TagResolver.standard())
                    .tag("error", Tag.styling(RED))
                    .tag("warn", Tag.styling(YELLOW))
                    .tag("info", Tag.styling(GREEN))
                    .tag("debug", Tag.styling(BLUE))
                    .build())
            .build();

    private static final net.kyori.adventure.key.Key TRANSLATION_KEY =
            net.kyori.adventure.key.Key.key("template_plugin", "messages");

    private static final String FILE_PREFIX = "messages";
    private static final String FILE_SUFFIX = ".properties";

    private final Path translationDir;
    private final ComponentLogger logger;

    @Inject
    private TranslatorLoader(
            final @DataDirectory Path dataDirectory,
            final ComponentLogger logger
    ) {
        this.translationDir = dataDirectory.resolve("translations");
        this.logger = logger;
        try {
            Files.createDirectories(this.translationDir);
        } catch (final IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    Translator loadTranslator() throws UncheckedIOException {
        this.logger.debug("[{}] Building translation store...", TranslatorLoader.class.getSimpleName());
        final var store = MiniMessageTranslationStore.create(TRANSLATION_KEY, MINI_MESSAGE);
        store.defaultLocale(Locale.ROOT);

        // 1. Register ROOT locale from compile-time annotations (ultimate fallback)
        final Map<String, String> rootTranslations = readAnnotations(MessageAssembly.class, Locale.ROOT)
                .messages()
                .stream()
                .collect(Collectors.toUnmodifiableMap(Translation.Message::key, Translation.Message::content));
        store.registerAll(Locale.ROOT, rootTranslations);
        this.logger.debug("[{}] Registered {} ROOT-locale messages from annotations.", TranslatorLoader.class.getSimpleName(), rootTranslations.size());

        // 2. Register all translation files present on disk (operator overrides)
        final Set<Locale> diskLocales = new HashSet<>();
        try (Stream<Path> files = Files.list(this.translationDir)) {
            files.filter(Files::isRegularFile)
                    .filter(TranslatorLoader::isTranslationFile)
                    .forEach(file -> {
                        final Locale locale = parseLocale(file);
                        store.registerAll(locale, file, false);
                        diskLocales.add(locale);
                        this.logger.debug("[{}] Loaded translation file: {} (locale: {})", TranslatorLoader.class.getSimpleName(), file.getFileName(), locale);
                    });
        } catch (final IOException exception) {
            throw new UncheckedIOException(exception);
        }

        if (diskLocales.isEmpty()) {
            this.logger.debug("[{}] No operator-provided translation files found in {}.", TranslatorLoader.class.getSimpleName(), this.translationDir);
        } else {
            this.logger.info("Loaded {} translation file(s) from disk: {}.", diskLocales.size(), diskLocales);
        }

        // 3. Fill in locales defined in annotations but absent on disk, and write them out
        int generatedFiles = 0;
        for (final Translation translation : readAllAnnotations(MessageAssembly.class)) {
            translation.messages().stream()
                    .filter(msg -> !store.contains(msg.key(), translation.locale()))
                    .forEach(msg -> store.register(msg.key(), translation.locale(), msg.content()));

            if (!diskLocales.contains(translation.locale())) {
                writeTranslationFile(this.translationDir, translation);
                generatedFiles++;
                this.logger.debug("[{}] Generated default translation file: {}.", TranslatorLoader.class.getSimpleName(), translation);
            }
        }
        if (generatedFiles > 0) {
            this.logger.info(
                    "Generated {} default translation file(s) — edit them in {} to customise messages.",
                    generatedFiles,
                    this.translationDir
            );
        }

        this.logger.debug("[{}] Translation store build complete.", TranslatorLoader.class.getSimpleName());
        return store;
    }

    private static List<Translation> readAllAnnotations(final Class<?> translationClass) {
        return Locale.availableLocales()
                .map(locale -> readAnnotations(translationClass, locale))
                .filter(translation -> !translation.messages().isEmpty())
                .sorted(Comparator.comparing(translation -> translation.locale().toString()))
                .toList();
    }

    private static Translation readAnnotations(final Class<?> translationClass, final Locale locale) {
        final List<Translation.Message> messages = new ArrayList<>();

        final List<Method> methods = Arrays.stream(translationClass.getMethods())
                .filter(method -> method.isAnnotationPresent(Key.class))
                .sorted(Comparator.comparing(method -> method.getAnnotation(Key.class).value()))
                .toList();

        for (final Method method : methods) {
            final String key = method.getAnnotation(Key.class).value();
            for (final Message msg : method.getAnnotationsByType(Message.class)) {
                if (locale.equals(msg.locale().asLocale())) {
                    messages.add(new Translation.Message(key, msg.content()));
                }
            }
        }

        return new Translation(locale, messages);
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

    private static String fileNameFromLocale(final Locale locale) {
        if (locale == Locale.ROOT) {
            return "";
        }
        return FILE_PREFIX + "_" + locale + FILE_SUFFIX;
    }

    private static void writeTranslationFile(final Path parentDir, final Translation translation) throws UncheckedIOException {
        final Locale locale = translation.locale() == Locale.ROOT ? Locale.US : translation.locale();
        final String fileName = fileNameFromLocale(locale);
        if (fileName.isEmpty()) {
            return;
        }
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

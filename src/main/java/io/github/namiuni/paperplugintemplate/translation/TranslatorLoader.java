/*
 * paper-plugin-template
 *
 * Copyright (c) 2026. Namiu/Unitarou
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
package io.github.namiuni.paperplugintemplate.translation;

import io.github.namiuni.kotonoha.annotations.Key;
import io.github.namiuni.kotonoha.annotations.Message;
import io.github.namiuni.paperplugintemplate.guice.DataDirectory;
import jakarta.inject.Inject;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.translation.MiniMessageTranslationStore;
import net.kyori.adventure.translation.Translator;
import org.jspecify.annotations.NullMarked;

/// Loads a fully initialised {@link Translator} from annotation-embedded defaults and
/// overrides stored in the plugin's translation directory.
///
/// <p>The loading strategy follows a three-step priority order:
/// <ol>
///   <li><b>ROOT locale</b> – always sourced from the compile-time annotations on
///       {@link TemplateMessages}; these act as the ultimate fallback.</li>
///   <li><b>Disk files</b> – {@code .properties} files found under the
///       {@code translation/} sub-directory override the annotation defaults for their
///       respective locales, allowing server operators to customise messages.</li>
///   <li><b>Annotation fill-in</b> – any locale defined in annotations but absent from
///       disk is registered programmatically and also written to disk so operators can
///       edit it later.</li>
/// </ol>
///
/// <p>Custom MiniMessage tags registered by this loader:
/// <ul>
///   <li>{@code <error>} – {@link #RED} (JIS Z 9103 safety red)</li>
///   <li>{@code <warn>}  – {@link #YELLOW} (JIS Z 9103 safety yellow)</li>
///   <li>{@code <info>}  – {@link #GREEN} (JIS Z 9103 safety green)</li>
///   <li>{@code <debug>} – {@link #BLUE} (JIS Z 9103 safety blue)</li>
/// </ul>
@NullMarked
final class TranslatorLoader {

    // JIS Z 9103 https://ja.wikipedia.org/wiki/JIS%E5%AE%89%E5%85%A8%E8%89%B2

    /// JIS Z 9103 safety red used for error messages.
    private static final TextColor RED = TextColor.color(0xFF4B00);

    /// JIS Z 9103 safety yellow used for warning messages.
    private static final TextColor YELLOW = TextColor.color(0xF2E700);

    /// JIS Z 9103 safety green used for informational messages.
    private static final TextColor GREEN = TextColor.color(0x00B06B);

    /// JIS Z 9103 safety blue used for debug messages.
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

    /// Constructs a new loader, creating the {@code translation/} directory if it does
    /// not already exist.
    ///
    /// @param dataDirectory the plugin data directory, injected via {@link DataDirectory}
    /// @throws UncheckedIOException if the translation directory cannot be created
    @Inject
    private TranslatorLoader(final @DataDirectory Path dataDirectory) {
        this.translationDir = dataDirectory.resolve("translation");
        try {
            Files.createDirectories(this.translationDir);
        } catch (final IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    /// Builds and returns a fresh {@link Translator} instance containing all registered
    /// message translations.
    ///
    /// <p>Calling this method more than once produces independent translator instances;
    /// the previous instance is not modified.
    ///
    /// @return a fully populated {@link Translator}
    /// @throws IOException if reading or writing translation files fails
    Translator loadTranslator() throws IOException {
        final var store = MiniMessageTranslationStore.create(TRANSLATION_KEY, MINI_MESSAGE);
        store.defaultLocale(Locale.ROOT);

        // 1. Register ROOT locale from compile-time annotations (ultimate fallback)
        final Map<String, String> rootTranslations = readAnnotations(TemplateMessages.class, Locale.ROOT)
                .messages()
                .stream()
                .collect(Collectors.toUnmodifiableMap(Translation.Message::key, Translation.Message::content));
        store.registerAll(Locale.ROOT, rootTranslations);

        // 2. Register all translation files present on disk (operator overrides)
        final Set<Locale> diskLocales = new HashSet<>();
        try (Stream<Path> files = Files.list(this.translationDir)) {
            files.filter(Files::isRegularFile)
                    .filter(TranslatorLoader::isTranslationFile)
                    .forEach(file -> {
                        final Locale locale = parseLocale(file);
                        store.registerAll(locale, file, false);
                        diskLocales.add(locale);
                    });
        }

        // 3. Fill in locales defined in annotations but absent on disk, and write them out
        for (final Translation translation : readAllAnnotations(TemplateMessages.class)) {
            translation.messages().stream()
                    .filter(msg -> !store.contains(msg.key(), translation.locale()))
                    .forEach(msg -> store.register(msg.key(), translation.locale(), msg.content()));

            if (!diskLocales.contains(translation.locale())) {
                writeTranslationFile(this.translationDir, translation);
            }
        }

        return store;
    }

    // -------------------------------------------------------------------------
    // Annotation reading
    // -------------------------------------------------------------------------

    /// Reads all non-empty {@link Translation} instances from the given interface
    /// across every locale available in the JVM.
    ///
    /// @param translationClass the interface annotated with {@link Key} and {@link Message}
    /// @return an unordered set of non-empty translations; one entry per locale
    private static Set<Translation> readAllAnnotations(final Class<?> translationClass) {
        return Locale.availableLocales()
                .map(locale -> readAnnotations(translationClass, locale))
                .filter(translation -> !translation.messages().isEmpty())
                .collect(Collectors.toSet());
    }

    /// Reads the {@link Translation} for a single locale from the given interface.
    ///
    /// @param translationClass the interface whose methods carry {@link Key} and {@link Message}
    /// @param locale           the target locale to extract messages for
    /// @return a {@link Translation} for the locale; message list may be empty
    private static Translation readAnnotations(final Class<?> translationClass, final Locale locale) {
        final List<Translation.Message> messages = new ArrayList<>();

        for (final var method : translationClass.getMethods()) {
            final Key keyAnnotation = method.getAnnotation(Key.class);
            if (keyAnnotation == null) {
                continue;
            }
            final String key = keyAnnotation.value();
            for (final Message msg : method.getAnnotationsByType(Message.class)) {
                if (locale.equals(msg.locale().asLocale())) {
                    messages.add(new Translation.Message(key, msg.content()));
                }
            }
        }

        return new Translation(locale, messages);
    }

    // -------------------------------------------------------------------------
    // File name helpers
    // -------------------------------------------------------------------------

    /// Returns {@code true} if the given path looks like a translation file.
    ///
    /// <p>A file matches when its name starts with {@value #FILE_PREFIX} and ends
    /// with {@value #FILE_SUFFIX}.
    ///
    /// @param file the path to test; only the file name component is examined
    /// @return {@code true} if the file is a candidate translation file
    private static boolean isTranslationFile(final Path file) {
        final String name = file.getFileName().toString();
        return name.startsWith(FILE_PREFIX) && name.endsWith(FILE_SUFFIX);
    }

    /// Parses and returns the {@link Locale} encoded in the given file's name.
    ///
    /// <p>The locale tag is the substring between the trailing {@code '_'} after
    /// {@value #FILE_PREFIX} and the leading {@code '.'} of {@value #FILE_SUFFIX}.
    /// For example, {@code messages_ja_JP.properties} yields {@code ja_JP}.
    ///
    /// @param file the translation file whose name encodes the locale
    /// @return the parsed locale
    /// @throws IllegalArgumentException if the locale tag cannot be resolved
    private static Locale parseLocale(final Path file) {
        final String name = file.getFileName().toString();
        final String tag = name.substring(FILE_PREFIX.length() + 1, name.length() - FILE_SUFFIX.length());
        final Locale locale = Translator.parseLocale(tag);
        if (locale == null) {
            throw new IllegalArgumentException("Cannot parse locale from translation file name: " + name);
        }
        return locale;
    }

    /// Returns the file name for the given locale.
    ///
    /// @param locale the locale to convert
    /// @return the file name (e.g. {@code messages_ja_JP.properties}),
    ///         or an empty string when {@code locale} is {@link Locale#ROOT}
    private static String fileNameFromLocale(final Locale locale) {
        if (locale == Locale.ROOT) {
            return "";
        }
        return FILE_PREFIX + "_" + locale + FILE_SUFFIX;
    }

    // -------------------------------------------------------------------------
    // File writing
    // -------------------------------------------------------------------------

    /// Writes a {@link Translation} to a {@code .properties} file inside {@code parentDir}.
    ///
    /// <p>{@link Locale#ROOT} is treated as {@link Locale#US} for the file name because
    /// the root locale has no standard BCP-47 tag. If the resulting file name would be
    /// empty this method returns without writing anything.
    ///
    /// @param parentDir   the directory in which the file will be created
    /// @param translation the translation to persist; any existing file is overwritten
    /// @throws IOException if the file cannot be created or written
    private static void writeTranslationFile(final Path parentDir, final Translation translation) throws IOException {
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
        }
    }
}

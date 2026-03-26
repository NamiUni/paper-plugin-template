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

import io.github.namiuni.paperplugintemplate.guice.DataDirectory;
import jakarta.inject.Inject;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.translation.MiniMessageTranslationStore;
import net.kyori.adventure.translation.Translator;
import org.jspecify.annotations.NullMarked;

/// Loads a fully initialised [Translator] from annotation-embedded defaults and
/// overrides stored in the plugin's translation directory.
///
/// The loading strategy follows a three-step priority order:
/// <ol>
///     - **ROOT locale** – always sourced from the compile-time annotations on
///     [TemplateMessages]; these act as the ultimate fallback.
///     - **Disk files** – `.properties` files found under the
///     `translation/` sub-directory override the annotation defaults for their
///     respective locales, allowing server operators to customise messages.
///     - **Annotation fill-in** – any locale defined in annotations but absent from
///     disk is registered programmatically and also written to disk so operators can
///     edit it later.
/// </ol>
///
/// Custom MiniMessage tags registered by this loader:
///
///     - `<e>` – [#RED] (JIS Z 9103 safety red)
///     - `<warn>` – [#YELLOW] (JIS Z 9103 safety yellow)
///     - `<info>` – [#GREEN] (JIS Z 9103 safety green)
///     - `<debug>` – [#BLUE] (JIS Z 9103 safety blue)
///
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

    private static final Key TRANSLATION_KEY = Key.key("template_plugin", "messages");

    private final Path translationDir;

    /// Constructs a new loader, creating the `translation/` directory if it does
    /// not already exist.
    ///
    /// @param dataDirectory the plugin data directory, injected via [DataDirectory]
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

    /// Builds and returns a fresh [Translator] instance containing all registered
    /// message translations.
    ///
    /// Calling this method more than once produces independent translator instances;
    /// the previous instance is not modified.
    ///
    /// @return a fully populated [Translator]
    /// @throws IOException if reading translation files from disk fails
    Translator loadTranslator() throws IOException {
        final var store = MiniMessageTranslationStore.create(TRANSLATION_KEY, MINI_MESSAGE);

        // 1. Register ROOT Locale
        final Map<String, String> rootTranslations = Translations.read(TemplateMessages.class, Locale.ROOT)
                .messages()
                .stream()
                .map(message -> Map.entry(message.key(), message.content()))
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));

        store.registerAll(Locale.ROOT, rootTranslations);

        // 2. Register all translation files on the disk
        final Set<Locale> diskLocales = new HashSet<>();
        try (Stream<Path> files = Files.list(this.translationDir)) {
            files.filter(Files::isRegularFile)
                    .filter(TranslationFileNames::matches)
                    .map(TranslationFile::of)
                    .forEach(translationFile -> {
                        store.registerAll(translationFile.locale(), translationFile.file(), false);
                        diskLocales.add(translationFile.locale());
                    });
        }

        // 3. Find and add locales that are not installed from the class
        for (final Translation translation : Translations.readAll(TemplateMessages.class)) {
            translation.messages().stream()
                    .filter(message -> !store.contains(message.key(), translation.locale()))
                    .forEach(message -> store.register(message.key(), translation.locale(), message.content()));

            if (!diskLocales.contains(translation.locale())) {
                TranslationWriter.write(this.translationDir, translation);
            }
        }

        return store;
    }
}

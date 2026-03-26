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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.jspecify.annotations.NullMarked;

/// Utility class for extracting [Translation] instances from annotated interfaces.
///
/// Methods in this class use reflection to read [Key] and [Message]
/// annotations from a translation interface (e.g. [TemplateMessages]) and convert
/// them into [Translation] value objects that can be registered with Adventure's
/// translation store or written to disk.
///
/// This class is non-instantiable; all methods are static.
@NullMarked
final class Translations {

    private Translations() {
    }

    /// Reads all non-empty [Translation] instances from the given annotated interface
    /// across every locale available in the JVM.
    ///
    /// Locales with no messages defined in `translationClass` are silently
    /// excluded from the returned set.
    ///
    /// @param translationClass the interface whose methods are annotated with
    ///                         [Key] and [Message]
    /// @return an unordered, immutable-like set of non-empty translations
    static Set<Translation> readAll(final Class<?> translationClass) {
        return Locale.availableLocales()
                .map(locale -> read(translationClass, locale))
                .filter(translation -> !translation.messages().isEmpty())
                .collect(Collectors.toSet());
    }

    /// Reads the [Translation] for a single locale from the given annotated interface.
    ///
    /// Each public method annotated with [Key] is inspected for [Message]
    /// annotations whose locale matches `locale`. Matching messages are collected
    /// into the returned [Translation]; the result may have an empty message list if
    /// no messages are defined for the requested locale.
    ///
    /// @param translationClass the interface whose methods carry the translation annotations
    /// @param locale           the target locale to extract messages for
    /// @return a [Translation] for the given locale; message list may be empty
    static Translation read(final Class<?> translationClass, final Locale locale) {
        final List<Translation.Message> messages = new ArrayList<>();

        for (final var method : translationClass.getMethods()) {
            final Key keyAnnotation = method.getAnnotation(Key.class);
            if (keyAnnotation == null) {
                continue;
            }

            final String key = keyAnnotation.value();
            for (final Message message : method.getAnnotationsByType(Message.class)) {
                if (locale.equals(message.locale().asLocale())) {
                    messages.add(new Translation.Message(key, message.content()));
                }
            }
        }

        return new Translation(locale, messages);
    }
}

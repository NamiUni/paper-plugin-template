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

import java.nio.file.Path;
import java.util.Locale;
import net.kyori.adventure.translation.Translator;
import org.jspecify.annotations.NullMarked;

/// Utility class for deriving and validating translation file names.
///
/// Translation files follow the naming convention
/// `messages_<locale-tag>.properties`, for example
/// `messages_ja_JP.properties`. This class encodes that convention in a single
/// place so that both the loader and writer remain consistent.
///
/// This class is non-instantiable; all members are static.
@NullMarked
final class TranslationFileNames {

    private static final String PREFIX = "messages";
    private static final String SUFFIX = ".properties";

    private TranslationFileNames() {
    }

    /// Returns `true` if the given file path looks like a translation file.
    ///
    /// A file matches if its name starts with `"messages"` and ends with
    /// `".properties"`.
    ///
    /// @param file the path to test; only the file name component is examined
    /// @return `true` if the file is a candidate translation file
    static boolean matches(final Path file) {
        final String name = file.getFileName().toString();
        return name.startsWith(PREFIX) && name.endsWith(SUFFIX);
    }

    /// Parses and returns the [Locale] encoded in the given file's name.
    ///
    /// The locale tag is the substring between the trailing `'_'` after
    /// `"messages"` and the leading `'.'` of `".properties"`.
    /// For example, `messages_ja_JP.properties` yields `ja_JP`.
    ///
    /// @param file the translation file whose name encodes the locale
    /// @return the parsed locale
    /// @throws ParsingException if the locale tag cannot be resolved by
    ///                          [Translator#parseLocale(String)]
    static Locale parseLocale(final Path file) throws ParsingException {
        final String name = file.getFileName().toString();
        final String localeTag = name.substring(PREFIX.length() + 1, name.length() - SUFFIX.length());
        final Locale locale = Translator.parseLocale(localeTag);
        if (locale == null) {
            throw new ParsingException("Failed to parse locale from filename: %s".formatted(name));
        }

        return locale;
    }

    /// Converts a [Locale] to the corresponding translation file name.
    ///
    /// [Locale#ROOT] is treated as a special case: the file name contains
    /// no locale suffix (i.e. the result is an empty string representing no file
    /// is written for root separately).
    ///
    /// @param locale the locale to convert
    /// @return the file name string, or an empty string for [Locale#ROOT]
    static String fromLocale(final Locale locale) {
        if (locale == Locale.ROOT) {
            return "";
        }
        return PREFIX + "_" + locale + SUFFIX;
    }
}

/*
 * paper-plugin-template
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
package io.github.namiuni.paperplugintemplate.translation;

import java.nio.file.Path;
import java.util.Locale;
import org.jspecify.annotations.NullMarked;

/// Immutable value object pairing a translation file path with its parsed locale.
///
/// Created by [TranslatorLoader] when scanning the plugin's translation
/// directory. The locale is derived from the file name via
/// [TranslationFileNames#parseLocale(Path)].
///
/// @param locale the locale encoded in the file name
/// @param file   the path to the translation properties file on disk
@NullMarked
record TranslationFile(Locale locale, Path file) {

    /// Creates a `TranslationFile` by parsing the locale from the given file path.
    ///
    /// @param file the translation properties file; its name must conform to the
    ///             pattern expected by [TranslationFileNames#parseLocale(Path)]
    /// @return a new `TranslationFile` with the resolved locale and path
    /// @throws ParsingException if the file name does not contain a parseable locale tag
    public static TranslationFile of(final Path file) {
        return new TranslationFile(TranslationFileNames.parseLocale(file), file);
    }
}

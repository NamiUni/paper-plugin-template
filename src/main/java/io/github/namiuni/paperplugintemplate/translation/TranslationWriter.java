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

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import org.jspecify.annotations.NullMarked;

/// Utility class for writing [Translation] objects to `.properties` files.
///
/// Each message is written as a single `key=content` line. The output file is
/// named using the convention defined by [TranslationFileNames]. If the locale is
/// [Locale#ROOT], the file is written as if it were [Locale#US] because the
/// root locale has no standard BCP-47 tag suitable for a file name.
///
/// This class is non-instantiable; all members are static.
@NullMarked
final class TranslationWriter {

    private TranslationWriter() {
    }

    /// Writes a [Translation] to a `.properties` file inside `parentDir`.
    ///
    /// The file name is derived from [Translation#locale()] via
    /// [TranslationFileNames#fromLocale(Locale)]. Any existing file with the same
    /// name is overwritten. The method does not create parent directories; the caller is
    /// responsible for ensuring that `parentDir` exists.
    ///
    /// @param parentDir   the directory in which the file will be created
    /// @param translation the translation to persist
    /// @throws IOException if the file cannot be created or written
    static void write(final Path parentDir, final Translation translation) throws IOException {
        final Locale locale = translation.locale() == Locale.ROOT ? Locale.US : translation.locale();
        final String fileName = TranslationFileNames.fromLocale(locale);
        final Path translationFile = parentDir.resolve(fileName);
        try (BufferedWriter writer = Files.newBufferedWriter(translationFile)) {
            for (final Translation.Message entry : translation.messages()) {
                writer.write(entry.key());
                writer.write('=');
                writer.write(entry.content());
                writer.newLine();
            }
        }
    }
}

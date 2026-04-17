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

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import org.jspecify.annotations.NullMarked;

@NullMarked
final class DefaultTranslationFileWriter {

    private static final String FILE_PREFIX = "messages";
    private static final String FILE_SUFFIX = ".properties";

    private DefaultTranslationFileWriter() {
    }

    static void write(final Path parentDir, final Translation translation) {
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

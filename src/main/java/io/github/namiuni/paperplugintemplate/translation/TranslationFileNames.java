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
import net.kyori.adventure.translation.Translator;
import org.jspecify.annotations.NullMarked;

@NullMarked
final class TranslationFileNames {

    private static final String PREFIX = "messages";
    private static final String SUFFIX = ".properties";

    private TranslationFileNames() {
    }

    static boolean matches(final Path file) {
        final String name = file.getFileName().toString();
        return name.startsWith(PREFIX) && name.endsWith(SUFFIX);
    }

    static Locale parseLocale(final Path file) throws ParsingException {
        final String name = file.getFileName().toString();
        final String localeTag = name.substring(PREFIX.length() + 1, name.length() - SUFFIX.length());
        final Locale locale = Translator.parseLocale(localeTag);
        if (locale == null) {
            throw new ParsingException("Failed to parse locale from filename: %s".formatted(name));
        }

        return locale;
    }

    static String fromLocale(final Locale locale) {
        if (locale == Locale.ROOT) {
            return "";
        }
        return PREFIX + "_" + locale + SUFFIX;
    }
}

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

import java.io.Serial;
import org.jspecify.annotations.NullMarked;

/// Thrown when a translation-related value (such as a locale tag embedded in a file name)
/// cannot be parsed into the expected type.
///
/// @see TranslationFileNames#parseLocale(java.nio.file.Path)
@NullMarked
public final class ParsingException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = -5299203683492438055L;

    /// Constructs a new `ParsingException` with the given detail message.
    ///
    /// @param message a human-readable description of the parsing failure
    public ParsingException(final String message) {
        super(message);
    }
}

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

import java.util.List;
import java.util.Locale;
import org.jspecify.annotations.NullMarked;

/// Immutable value object representing a complete set of messages for one locale.
///
/// A `Translation` groups all [Message] entries that belong to a
/// specific [Locale]. It is produced by [TranslatorLoader] and consumed
/// when registering messages into Adventure's translation store and when persisting
/// them to disk.
///
/// @param locale   the locale these messages apply to
/// @param messages the ordered list of key-value message pairs for this locale
@NullMarked
record Translation(Locale locale, List<Message> messages) {

    /// A single translation entry consisting of a message key and its localized content.
    ///
    /// @param key     the dot-separated translation key
    /// @param content the MiniMessage-formatted string for this key
    record Message(String key, String content) {
    }
}

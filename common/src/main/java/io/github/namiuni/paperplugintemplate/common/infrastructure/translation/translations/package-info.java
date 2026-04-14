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

/// Centralised message definitions used throughout the plugin.
///
/// This package contains
/// [io.github.namiuni.paperplugintemplate.common.infrastructure.translation.translations.MessageAssembly],
/// a Kotonoha-proxied interface where each method declaration is both the API surface for
/// retrieving a localized [net.kyori.adventure.text.Component] and the canonical source
/// of the compile-time default translation strings (via `@Key` and `@Message` annotations).
///
/// ## Locales
///
/// Translations are provided for the following locales by default:
///
/// | Locale | Language |
/// |---|---|
/// | `ROOT` (`en`) | English (fallback) |
/// | `ja_JP`       | Japanese           |
///
/// Additional locales are supported by placing `.properties` override files in the
/// `<dataDirectory>/translations/` directory. Missing keys fall back to the ROOT locale.
///
/// ## Adding a new message
///
/// 1. Add a method to `MessageAssembly` annotated with `@Key`, `@Message` (for each
///    supported locale), and the appropriate `@WithPlaceholders` scope.
/// 2. The Kotonoha proxy will automatically resolve the message at runtime, and
///    the translation infrastructure will write the new key to the on-disk files
///    the next time the plugin starts.
package io.github.namiuni.paperplugintemplate.common.infrastructure.translation.translations;

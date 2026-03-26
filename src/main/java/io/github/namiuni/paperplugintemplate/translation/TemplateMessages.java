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
import io.github.namiuni.kotonoha.annotations.Locales;
import io.github.namiuni.kotonoha.annotations.Message;
import io.github.namiuni.kotonoha.annotations.ResourceBundle;
import io.github.namiuni.kotonoha.translatable.message.extra.miniplaceholders.PlaceholderScope;
import io.github.namiuni.kotonoha.translatable.message.extra.miniplaceholders.WithPlaceholders;
import net.kyori.adventure.text.Component;
import org.jspecify.annotations.NullMarked;

/// Centralised message definitions for the template plugin.
///
/// Each method corresponds to a single translatable message identified by its
/// [Key] annotation. Locale-specific content is declared via [Message]
/// annotations; the Kotonoha library generates a proxy implementation that resolves
/// the appropriate locale at call time.
///
/// Instances are obtained from the Guice injector; use constructor injection
/// rather than accessing this interface statically.
@NullMarked
@ResourceBundle(baseName = "messages")
public interface TemplateMessages {

    /// Returns the message sent to a command sender when the configuration is
    /// reloaded successfully.
    ///
    /// @return a localized [Component] indicating success
    @Key("template.command.reload.config.success")
    @Message(locale = Locales.ROOT, content = "<info>Configuration reloaded successfully.")
    @Message(locale = Locales.JA_JP, content = "<info>設定の再読み込みに成功しました。")
    @WithPlaceholders(PlaceholderScope.AUDIENCE_GLOBAL)
    Component configurationReloadSuccess();

    /// Returns the message sent to a command sender when the configuration reload fails.
    ///
    /// @return a localized [Component] indicating failure
    @Key("template.command.reload.config.failure")
    @Message(locale = Locales.ROOT, content = "<error>Failed to reload configuration. See the console for details.")
    @Message(locale = Locales.JA_JP, content = "<error>設定の再読み込みに失敗しました。詳細はコンソールを確認してください。")
    @WithPlaceholders(PlaceholderScope.AUDIENCE_GLOBAL)
    Component configurationReloadFailure();

    /// Returns the message sent to a command sender when the translation files are
    /// reloaded successfully.
    ///
    /// @return a localized [Component] indicating success
    @Key("template.command.reload.translation.success")
    @Message(locale = Locales.ROOT, content = "<info>Configuration reloaded successfully.")
    @Message(locale = Locales.JA_JP, content = "<info>翻訳の再読み込みに成功しました。")
    @WithPlaceholders(PlaceholderScope.AUDIENCE_GLOBAL)
    Component translationReloadSuccess();

    /// Returns the message sent to a command sender when the translation reload fails.
    ///
    /// @return a localized [Component] indicating failure
    @Key("template.command.reload.translation.failure")
    @Message(locale = Locales.ROOT, content = "<error>Failed to reload configuration. See the console for details.")
    @Message(locale = Locales.JA_JP, content = "<error>翻訳の再読み込みに失敗しました。詳細はコンソールを確認してください。")
    @WithPlaceholders(PlaceholderScope.AUDIENCE_GLOBAL)
    Component translationReloadFailure();
}

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
package io.github.namiuni.paperplugintemplate.common.infrastructure.translation.translations;

import io.github.namiuni.kotonoha.annotations.Key;
import io.github.namiuni.kotonoha.annotations.Locales;
import io.github.namiuni.kotonoha.annotations.Message;
import io.github.namiuni.kotonoha.annotations.ResourceBundle;
import io.github.namiuni.kotonoha.translatable.message.extra.miniplaceholders.PlaceholderScope;
import io.github.namiuni.kotonoha.translatable.message.extra.miniplaceholders.WithPlaceholders;
import net.kyori.adventure.pointer.Pointered;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.jspecify.annotations.NullMarked;

@NullMarked
@ResourceBundle(baseName = "messages")
public interface MessageAssembly {

    /// Returns the short description of the `/template reload` command.
    ///
    /// @return a localized [Component] describing the reload command
    @Key("template.command.reload.description")
    @Message(locale = Locales.ROOT, content = "Reloads plugin configuration.")
    @Message(locale = Locales.JA_JP, content = "プラグインの設定を再読み込みします。")
    @WithPlaceholders(PlaceholderScope.GLOBAL)
    Component commandReloadDescription();

    /// Returns the message sent when the configuration is reloaded successfully.
    ///
    /// @return a localized [Component] indicating success
    @Key("template.config.reload.success")
    @Message(locale = Locales.ROOT, content = "<info>Configuration reloaded successfully.")
    @Message(locale = Locales.JA_JP, content = "<info>設定の再読み込みに成功しました。")
    @WithPlaceholders(PlaceholderScope.AUDIENCE_GLOBAL)
    Component configurationReloadSuccess(Pointered pointered);

    /// Returns the message sent when the configuration reload fails.
    ///
    /// @return a localized [Component] indicating failure
    @Key("template.config.reload.failure")
    @Message(locale = Locales.ROOT, content = "<error>Failed to reload configuration. See the console for details.")
    @Message(locale = Locales.JA_JP, content = "<error>設定の再読み込みに失敗しました。詳細はコンソールを確認してください。")
    @WithPlaceholders(PlaceholderScope.AUDIENCE_GLOBAL)
    Component configurationReloadFailure(Pointered pointered);

    /// Returns the message sent when translation files are reloaded successfully.
    ///
    /// @return a localized [Component] indicating success
    @Key("template.translation.reload.success")
    @Message(locale = Locales.ROOT, content = "<info>Translations reloaded successfully.")
    @Message(locale = Locales.JA_JP, content = "<info>翻訳の再読み込みに成功しました。")
    @WithPlaceholders(PlaceholderScope.AUDIENCE_GLOBAL)
    Component translationReloadSuccess(Pointered pointered);

    /// Returns the message sent when the translation reload fails.
    ///
    /// @return a localized [Component] indicating failure
    @Key("template.translation.reload.failure")
    @Message(locale = Locales.ROOT, content = "<error>Failed to reload translations. See the console for details.")
    @Message(locale = Locales.JA_JP, content = "<error>翻訳の再読み込みに失敗しました。詳細はコンソールを確認してください。")
    @WithPlaceholders(PlaceholderScope.AUDIENCE_GLOBAL)
    Component translationReloadFailure(Pointered pointered);

    /// Returns the message sent to a player when their profile cannot be loaded on join.
    ///
    /// @return a localized [Component] indicating a profile load failure
    @Key("template.join.failure.profile")
    @Message(locale = Locales.ROOT, content = "<error>Failed to load your profile. Please try reconnecting.")
    @Message(locale = Locales.JA_JP, content = "<error>プロフィールの読み込みに失敗しました。再接続してください。")
    @WithPlaceholders(PlaceholderScope.AUDIENCE_GLOBAL)
    Component joinFailureProfile(Pointered pointered);

    /// Returns the short description of the `/template help` command.
    ///
    /// @return a localized [Component] describing the help command
    @Key("template.command.help.description")
    @Message(locale = Locales.ROOT, content = "Displays help for plugin commands.")
    @Message(locale = Locales.JA_JP, content = "プラグインコマンドのヘルプを表示します。")
    @WithPlaceholders(PlaceholderScope.GLOBAL)
    Component commandHelpDescription();

    /// Returns the "Arguments" section header label for MinecraftHelp.
    ///
    /// @param placeholders tag resolver supplied by MinecraftHelp
    /// @return a localized [Component]
    @Key("template.command.help.misc.arguments")
    @Message(locale = Locales.ROOT, content = "Arguments")
    @Message(locale = Locales.JA_JP, content = "引数")
    @WithPlaceholders(PlaceholderScope.AUDIENCE_GLOBAL)
    Component commandHelpMiscArguments(Pointered pointered, TagResolver placeholders);

    /// Returns the "Available Commands" heading for MinecraftHelp.
    ///
    /// @param placeholders tag resolver supplied by MinecraftHelp
    /// @return a localized [Component]
    @Key("template.command.help.misc.available_commands")
    @Message(locale = Locales.ROOT, content = "Available Commands")
    @Message(locale = Locales.JA_JP, content = "利用可能なコマンド")
    @WithPlaceholders(PlaceholderScope.AUDIENCE_GLOBAL)
    Component commandHelpMiscAvailableCommands(Pointered pointered, TagResolver placeholders);

    /// Returns the next-page navigation link for MinecraftHelp.
    ///
    /// @param placeholders tag resolver supplied by MinecraftHelp
    /// @return a localized [Component]
    @Key("template.command.help.misc.click_for_next_page")
    @Message(locale = Locales.ROOT, content = "Click for next page")
    @Message(locale = Locales.JA_JP, content = "クリックして次のページへ")
    @WithPlaceholders(PlaceholderScope.AUDIENCE_GLOBAL)
    Component commandHelpMiscClickForNextPage(Pointered pointered, TagResolver placeholders);

    /// Returns the previous-page navigation link for MinecraftHelp.
    ///
    /// @param placeholders tag resolver supplied by MinecraftHelp
    /// @return a localized [Component]
    @Key("template.command.help.misc.click_for_previous_page")
    @Message(locale = Locales.ROOT, content = "Click for previous page")
    @Message(locale = Locales.JA_JP, content = "クリックして前のページへ")
    @WithPlaceholders(PlaceholderScope.AUDIENCE_GLOBAL)
    Component commandHelpMiscClickForPreviousPage(Pointered pointered, TagResolver placeholders);

    /// Returns the click-to-show-help tooltip for MinecraftHelp.
    ///
    /// @param placeholders tag resolver supplied by MinecraftHelp
    /// @return a localized [Component]
    @Key("template.command.help.misc.click_to_show_help")
    @Message(locale = Locales.ROOT, content = "Click to show help for this command")
    @Message(locale = Locales.JA_JP, content = "クリックしてこのコマンドのヘルプを表示")
    @WithPlaceholders(PlaceholderScope.AUDIENCE_GLOBAL)
    Component commandHelpMiscClickToShowHelp(Pointered pointered, TagResolver placeholders);

    /// Returns the "Command" field label for MinecraftHelp.
    ///
    /// @param placeholders tag resolver supplied by MinecraftHelp
    /// @return a localized [Component]
    @Key("template.command.help.misc.command")
    @Message(locale = Locales.ROOT, content = "Command")
    @Message(locale = Locales.JA_JP, content = "コマンド")
    @WithPlaceholders(PlaceholderScope.AUDIENCE_GLOBAL)
    Component commandHelpMiscCommand(Pointered pointered, TagResolver placeholders);

    /// Returns the "Description" field label for MinecraftHelp.
    ///
    /// @param placeholders tag resolver supplied by MinecraftHelp
    /// @return a localized [Component]
    @Key("template.command.help.misc.description")
    @Message(locale = Locales.ROOT, content = "Description")
    @Message(locale = Locales.JA_JP, content = "説明")
    @WithPlaceholders(PlaceholderScope.AUDIENCE_GLOBAL)
    Component commandHelpMiscDescription(Pointered pointered, TagResolver placeholders);

    /// Returns the help section heading for MinecraftHelp.
    ///
    /// @param placeholders tag resolver supplied by MinecraftHelp
    /// @return a localized [Component]
    @Key("template.command.help.misc.help")
    @Message(locale = Locales.ROOT, content = "Help")
    @Message(locale = Locales.JA_JP, content = "ヘルプ")
    @WithPlaceholders(PlaceholderScope.AUDIENCE_GLOBAL)
    Component commandHelpMiscHelp(Pointered pointered, TagResolver placeholders);

    /// Returns the no-description fallback for MinecraftHelp.
    ///
    /// @param placeholders tag resolver supplied by MinecraftHelp
    /// @return a localized [Component]
    @Key("template.command.help.misc.no_description")
    @Message(locale = Locales.ROOT, content = "No description")
    @Message(locale = Locales.JA_JP, content = "説明なし")
    @WithPlaceholders(PlaceholderScope.AUDIENCE_GLOBAL)
    Component commandHelpMiscNoDescription(Pointered pointered, TagResolver placeholders);

    /// Returns the no-results message for MinecraftHelp.
    ///
    /// @param placeholders tag resolver supplied by MinecraftHelp
    /// @return a localized [Component]
    @Key("template.command.help.misc.no_results_for_query")
    @Message(locale = Locales.ROOT, content = "No results for query")
    @Message(locale = Locales.JA_JP, content = "クエリの結果はありません")
    @WithPlaceholders(PlaceholderScope.AUDIENCE_GLOBAL)
    Component commandHelpMiscNoResultsForQuery(Pointered pointered, TagResolver placeholders);

    /// Returns the optional-argument label for MinecraftHelp.
    ///
    /// @param placeholders tag resolver supplied by MinecraftHelp
    /// @return a localized [Component]
    @Key("template.command.help.misc.optional")
    @Message(locale = Locales.ROOT, content = "Optional")
    @Message(locale = Locales.JA_JP, content = "オプション")
    @WithPlaceholders(PlaceholderScope.AUDIENCE_GLOBAL)
    Component commandHelpMiscOptional(Pointered pointered, TagResolver placeholders);

    /// Returns the page-out-of-range error message for MinecraftHelp.
    ///
    /// The `placeholders` resolver provides `<page>` and `<max_pages>` tags.
    ///
    /// @param placeholders tag resolver providing `<page>` and `<max_pages>`
    /// @return a localized [Component]
    @Key("template.command.help.misc.page_out_of_range")
    @Message(locale = Locales.ROOT, content = "Error: Page <page> is not in range. Must be in range [1, <max_pages>]")
    @Message(locale = Locales.JA_JP, content = "エラー: ページ <page> は範囲外です。[1, <max_pages>] の範囲内でなければなりません。")
    @WithPlaceholders(PlaceholderScope.AUDIENCE_GLOBAL)
    Component commandHelpMiscPageOutOfRange(Pointered pointered, TagResolver placeholders);

    /// Returns the search-results header for MinecraftHelp.
    ///
    /// @param placeholders tag resolver supplied by MinecraftHelp
    /// @return a localized [Component]
    @Key("template.command.help.misc.showing_results_for_query")
    @Message(locale = Locales.ROOT, content = "Showing search results for query")
    @Message(locale = Locales.JA_JP, content = "クエリの検索結果を表示")
    @WithPlaceholders(PlaceholderScope.AUDIENCE_GLOBAL)
    Component commandHelpMiscShowingResultsForQuery(Pointered pointered, TagResolver placeholders);
}

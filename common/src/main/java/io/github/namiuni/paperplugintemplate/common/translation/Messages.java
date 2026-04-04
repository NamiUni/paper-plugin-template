/*
 * PaperPluginTemplate
 *
 * Copyright (c) 2026. Namiu (うにたろう)
 *                     Contributors
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
package io.github.namiuni.paperplugintemplate.common.translation;

import io.github.namiuni.kotonoha.annotations.Key;
import io.github.namiuni.kotonoha.annotations.Locales;
import io.github.namiuni.kotonoha.annotations.Message;
import io.github.namiuni.kotonoha.annotations.ResourceBundle;
import io.github.namiuni.kotonoha.translatable.message.extra.miniplaceholders.PlaceholderScope;
import io.github.namiuni.kotonoha.translatable.message.extra.miniplaceholders.WithPlaceholders;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.jspecify.annotations.NullMarked;

/// Centralised message definitions for the template plugin.
///
/// Each method corresponds to a single translatable message identified by its
/// [Key] annotation. Locale-specific content is declared via [Message]
/// annotations; the Kotonoha library generates a proxy implementation that
/// resolves the appropriate locale at call time.
///
/// Instances are obtained from the Guice injector; use constructor injection
/// rather than accessing this interface statically.
///
/// ## Thread safety
///
/// The Kotonoha-generated proxy is stateless and therefore safe to call from
/// any thread without additional synchronization.
@NullMarked
@ResourceBundle(baseName = "messages")
public interface Messages {

    /// Returns the message sent to a command sender when the configuration is reloaded successfully.
    ///
    /// @return a localized [Component] indicating success
    @Key("template.config.reload.success")
    @Message(locale = Locales.ROOT, content = "<info>Configuration reloaded successfully.")
    @Message(locale = Locales.JA_JP, content = "<info>設定の再読み込みに成功しました。")
    @WithPlaceholders(PlaceholderScope.AUDIENCE_GLOBAL)
    Component configurationReloadSuccess();

    /// Returns the message sent to a command sender when the configuration reload fails.
    ///
    /// @return a localized [Component] indicating failure
    @Key("template.config.reload.failure")
    @Message(locale = Locales.ROOT, content = "<error>Failed to reload configuration. See the console for details.")
    @Message(locale = Locales.JA_JP, content = "<error>設定の再読み込みに失敗しました。詳細はコンソールを確認してください。")
    @WithPlaceholders(PlaceholderScope.AUDIENCE_GLOBAL)
    Component configurationReloadFailure();

    /// Returns the message sent to a command sender when the translation files are reloaded successfully.
    ///
    /// @return a localized [Component] indicating success
    @Key("template.translation.reload.success")
    @Message(locale = Locales.ROOT, content = "<info>Configuration reloaded successfully.")
    @Message(locale = Locales.JA_JP, content = "<info>翻訳の再読み込みに成功しました。")
    @WithPlaceholders(PlaceholderScope.AUDIENCE_GLOBAL)
    Component translationReloadSuccess();

    /// Returns the message sent to a command sender when the translation reload fails.
    ///
    /// @return a localized [Component] indicating failure
    @Key("template.translation.reload.failure")
    @Message(locale = Locales.ROOT, content = "<error>Failed to reload configuration. See the console for details.")
    @Message(locale = Locales.JA_JP, content = "<error>翻訳の再読み込みに失敗しました。詳細はコンソールを確認してください。")
    @WithPlaceholders(PlaceholderScope.AUDIENCE_GLOBAL)
    Component translationReloadFailure();

    /// Returns the message sent to a player when their profile cannot be loaded on join.
    ///
    /// @return a localized [Component] indicating a profile load failure
    @Key("template.join.failure.profile")
    @Message(locale = Locales.ROOT, content = "<error>Failed to load your profile. Please try reconnecting.")
    @Message(locale = Locales.JA_JP, content = "<error>プロフィールの読み込みに失敗しました。再接続してください。")
    @WithPlaceholders(PlaceholderScope.AUDIENCE_GLOBAL)
    Component joinFailureProfile();

    /// Returns the short description of the `/template help` command shown in
    /// the help index and command introspection.
    ///
    /// Rendered by [org.incendo.cloud.description.CommandDescription] via
    /// [org.incendo.cloud.minecraft.extras.RichDescription], so Adventure
    /// formatting tags in the value are interpreted correctly.
    ///
    /// @return a localized [Component] describing the help command
    @Key("template.command.help.description")
    @Message(locale = Locales.ROOT, content = "Displays help for plugin commands.")
    @Message(locale = Locales.JA_JP, content = "プラグインコマンドのヘルプを表示します。")
    @WithPlaceholders(PlaceholderScope.AUDIENCE_GLOBAL)
    Component commandHelpDescription();

    /// Returns the label used for the "Arguments" section header in the
    /// [org.incendo.cloud.minecraft.extras.MinecraftHelp] verbose detail view.
    ///
    /// `placeholders` may carry dynamic values injected by MinecraftHelp;
    /// pass them to the resolver so that any `<…>` tags in the translated
    /// string are resolved correctly.
    ///
    /// @param placeholders tag resolver containing key-value pairs supplied
    ///                     by MinecraftHelp for this message key
    /// @return a localized [Component] for the arguments section label
    @Key("template.command.help.misc.arguments")
    @Message(locale = Locales.ROOT, content = "Arguments")
    @Message(locale = Locales.JA_JP, content = "引数")
    @WithPlaceholders(PlaceholderScope.AUDIENCE_GLOBAL)
    Component commandHelpMiscArguments(TagResolver placeholders);

    /// Returns the heading displayed above the list of available commands in
    /// the [org.incendo.cloud.minecraft.extras.MinecraftHelp] index view.
    ///
    /// @param placeholders tag resolver containing key-value pairs supplied
    ///                     by MinecraftHelp for this message key
    /// @return a localized [Component] for the available-commands heading
    @Key("template.command.help.misc.available_commands")
    @Message(locale = Locales.ROOT, content = "Available Commands")
    @Message(locale = Locales.JA_JP, content = "利用可能なコマンド")
    @WithPlaceholders(PlaceholderScope.AUDIENCE_GLOBAL)
    Component commandHelpMiscAvailableCommands(TagResolver placeholders);

    /// Returns the clickable navigation text that advances to the next page in
    /// the [org.incendo.cloud.minecraft.extras.MinecraftHelp] paginated view.
    ///
    /// @param placeholders tag resolver containing key-value pairs supplied
    ///                     by MinecraftHelp for this message key
    /// @return a localized [Component] for the next-page navigation link
    @Key("template.command.help.misc.click_for_next_page")
    @Message(locale = Locales.ROOT, content = "Click for next page")
    @Message(locale = Locales.JA_JP, content = "クリックして次のページへ")
    @WithPlaceholders(PlaceholderScope.AUDIENCE_GLOBAL)
    Component commandHelpMiscClickForNextPage(TagResolver placeholders);

    /// Returns the clickable navigation text that returns to the previous page
    /// in the [org.incendo.cloud.minecraft.extras.MinecraftHelp] paginated view.
    ///
    /// @param placeholders tag resolver containing key-value pairs supplied
    ///                     by MinecraftHelp for this message key
    /// @return a localized [Component] for the previous-page navigation link
    @Key("template.command.help.misc.click_for_previous_page")
    @Message(locale = Locales.ROOT, content = "Click for previous page")
    @Message(locale = Locales.JA_JP, content = "クリックして前のページへ")
    @WithPlaceholders(PlaceholderScope.AUDIENCE_GLOBAL)
    Component commandHelpMiscClickForPreviousPage(TagResolver placeholders);

    /// Returns the tooltip text shown when hovering over a command entry in the
    /// [org.incendo.cloud.minecraft.extras.MinecraftHelp] index, prompting the
    /// player to click in order to view detailed help for that command.
    ///
    /// @param placeholders tag resolver containing key-value pairs supplied
    ///                     by MinecraftHelp for this message key
    /// @return a localized [Component] for the click-to-show-help tooltip
    @Key("template.command.help.misc.click_to_show_help")
    @Message(locale = Locales.ROOT, content = "Click to show help for this command")
    @Message(locale = Locales.JA_JP, content = "クリックしてこのコマンドのヘルプを表示")
    @WithPlaceholders(PlaceholderScope.AUDIENCE_GLOBAL)
    Component commandHelpMiscClickToShowHelp(TagResolver placeholders);

    /// Returns the label for the "Command" field in the
    /// [org.incendo.cloud.minecraft.extras.MinecraftHelp] verbose detail view.
    ///
    /// @param placeholders tag resolver containing key-value pairs supplied
    ///                     by MinecraftHelp for this message key
    /// @return a localized [Component] for the command field label
    @Key("template.command.help.misc.command")
    @Message(locale = Locales.ROOT, content = "Command")
    @Message(locale = Locales.JA_JP, content = "コマンド")
    @WithPlaceholders(PlaceholderScope.AUDIENCE_GLOBAL)
    Component commandHelpMiscCommand(TagResolver placeholders);

    /// Returns the label for the "Description" field in the
    /// [org.incendo.cloud.minecraft.extras.MinecraftHelp] verbose detail view.
    ///
    /// @param placeholders tag resolver containing key-value pairs supplied
    ///                     by MinecraftHelp for this message key
    /// @return a localized [Component] for the description field label
    @Key("template.command.help.misc.description")
    @Message(locale = Locales.ROOT, content = "Description")
    @Message(locale = Locales.JA_JP, content = "説明")
    @WithPlaceholders(PlaceholderScope.AUDIENCE_GLOBAL)
    Component commandHelpMiscDescription(TagResolver placeholders);

    /// Returns the section heading rendered at the top of the
    /// [org.incendo.cloud.minecraft.extras.MinecraftHelp] index and detail
    /// views (e.g., "Help").
    ///
    /// @param placeholders tag resolver containing key-value pairs supplied
    ///                     by MinecraftHelp for this message key
    /// @return a localized [Component] for the help section heading
    @Key("template.command.help.misc.help")
    @Message(locale = Locales.ROOT, content = "Help")
    @Message(locale = Locales.JA_JP, content = "ヘルプ")
    @WithPlaceholders(PlaceholderScope.AUDIENCE_GLOBAL)
    Component commandHelpMiscHelp(TagResolver placeholders);

    /// Returns the placeholder text displayed when a command has no description
    /// registered in the [org.incendo.cloud.minecraft.extras.MinecraftHelp]
    /// detail view.
    ///
    /// @param placeholders tag resolver containing key-value pairs supplied
    ///                     by MinecraftHelp for this message key
    /// @return a localized [Component] for the no-description fallback
    @Key("template.command.help.misc.no_description")
    @Message(locale = Locales.ROOT, content = "No description")
    @Message(locale = Locales.JA_JP, content = "説明なし")
    @WithPlaceholders(PlaceholderScope.AUDIENCE_GLOBAL)
    Component commandHelpMiscNoDescription(TagResolver placeholders);

    /// Returns the message displayed when a search query in
    /// [org.incendo.cloud.minecraft.extras.MinecraftHelp] produces no matching
    /// commands.
    ///
    /// @param placeholders tag resolver containing key-value pairs supplied
    ///                     by MinecraftHelp for this message key
    /// @return a localized [Component] for the no-results empty state
    @Key("template.command.help.misc.no_results_for_query")
    @Message(locale = Locales.ROOT, content = "No results for query")
    @Message(locale = Locales.JA_JP, content = "クエリの結果はありません")
    @WithPlaceholders(PlaceholderScope.AUDIENCE_GLOBAL)
    Component commandHelpMiscNoResultsForQuery(TagResolver placeholders);

    /// Returns the label used to mark optional arguments in the
    /// [org.incendo.cloud.minecraft.extras.MinecraftHelp] syntax display.
    ///
    /// @param placeholders tag resolver containing key-value pairs supplied
    ///                     by MinecraftHelp for this message key
    /// @return a localized [Component] for the optional argument label
    @Key("template.command.help.misc.optional")
    @Message(locale = Locales.ROOT, content = "Optional")
    @Message(locale = Locales.JA_JP, content = "オプション")
    @WithPlaceholders(PlaceholderScope.AUDIENCE_GLOBAL)
    Component commandHelpMiscOptional(TagResolver placeholders);

    /// Returns the error message displayed when the requested page number falls
    /// outside the valid range in the [org.incendo.cloud.minecraft.extras.MinecraftHelp]
    /// paginated view.
    ///
    /// The `placeholders` resolver carries two dynamic tags injected by
    /// MinecraftHelp: `<page>` (the requested page number) and
    /// `<max_pages>` (the total number of available pages). Both must be
    /// present in the translated string to produce a meaningful error.
    ///
    /// @param placeholders tag resolver providing `<page>` and `<max_pages>`
    ///                     values from MinecraftHelp
    /// @return a localized [Component] describing the out-of-range page error
    @Key("template.command.help.misc.page_out_of_range")
    @Message(locale = Locales.ROOT, content = "Error: Page <page> is not in range. Must be in range [1, <max_pages>]")
    @Message(locale = Locales.JA_JP, content = "エラー: ページ <page> は範囲外です。[1, <max_pages>] の範囲内でなければなりません。")
    @WithPlaceholders(PlaceholderScope.AUDIENCE_GLOBAL)
    Component commandHelpMiscPageOutOfRange(TagResolver placeholders);

    /// Returns the header text shown above the filtered results when a player
    /// supplies a non-empty query to
    /// [org.incendo.cloud.minecraft.extras.MinecraftHelp].
    ///
    /// @param placeholders tag resolver containing key-value pairs supplied
    ///                     by MinecraftHelp for this message key
    /// @return a localized [Component] for the search-results header
    @Key("template.command.help.misc.showing_results_for_query")
    @Message(locale = Locales.ROOT, content = "Showing search results for query")
    @Message(locale = Locales.JA_JP, content = "クエリの検索結果を表示")
    @WithPlaceholders(PlaceholderScope.AUDIENCE_GLOBAL)
    Component commandHelpMiscShowingResultsForQuery(TagResolver placeholders);

    /// Returns the short description of the `/template reload` command shown in
    /// the help index and command introspection.
    ///
    /// Rendered by [org.incendo.cloud.description.CommandDescription] via
    /// [org.incendo.cloud.minecraft.extras.RichDescription], so Adventure
    /// formatting tags in the value are interpreted correctly.
    ///
    /// @return a localized [Component] describing the reload command
    @Key("template.command.reload.description")
    @Message(locale = Locales.ROOT, content = "Reloads plugin configuration.")
    @Message(locale = Locales.JA_JP, content = "プラグインの設定を再読み込みします。")
    @WithPlaceholders(PlaceholderScope.AUDIENCE_GLOBAL)
    Component commandReloadDescription();
}

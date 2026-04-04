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
package io.github.namiuni.paperplugintemplate.common.command.commands;

import io.github.namiuni.paperplugintemplate.common.command.CommandSource;
import io.github.namiuni.paperplugintemplate.common.permission.TemplatePermission;
import io.github.namiuni.paperplugintemplate.common.translation.Messages;
import jakarta.inject.Inject;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.component.DefaultValue;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.description.CommandDescription;
import org.incendo.cloud.help.result.CommandEntry;
import org.incendo.cloud.minecraft.extras.MinecraftHelp;
import org.incendo.cloud.minecraft.extras.RichDescription;
import org.incendo.cloud.parser.standard.StringParser;
import org.incendo.cloud.suggestion.BlockingSuggestionProvider;
import org.incendo.cloud.suggestion.Suggestion;
import org.jspecify.annotations.NullMarked;

/// [CommandFactory] that contributes the `/template help [query]` command.
///
/// Uses [MinecraftHelp] from `cloud-minecraft-extras` to render an
/// Adventure-styled, clickable help menu.
///
/// ## Command structure
///
/// ```
/// /template help             -- shows the root index
/// /template help <query>     -- shows verbose info for a specific command
/// ```
///
/// The `<query>` argument is optional and defaults to `""`, which triggers
/// the index view. When `<query>` matches a full command syntax string the
/// verbose view is displayed instead.
///
/// Tab-completion for `<query>` is sourced from
/// [org.incendo.cloud.help.HelpHandler#queryRootIndex] so suggestions
/// always reflect the live command tree and respect per-sender permissions.
///
/// ## Thread safety
///
/// [#command()] is invoked once on the bootstrap thread. Both the
/// [SuggestionProvider] callback and the command handler execute on
/// Cloud's async execution coordinator (a virtual thread) and are safe to
/// call from any thread.
@NullMarked
public final class HelpCommand implements CommandFactory {

    private static final TextColor PRIMARY = TextColor.color(0x2D7D9A);
    private static final TextColor HIGHLIGHT = TextColor.color(0x49E1E8);
    private static final TextColor ALT_HIGHLIGHT = TextColor.color(0xE3008C);
    private static final TextColor TEXT = TextColor.color(0xFFFFFF);
    private static final TextColor ACCENT = TextColor.color(0x7D7D7D);
    private static final String COMMAND_NAME = "template"; // TODO: change

    private final CommandManager<CommandSource> manager;
    private final Messages messages;
    private final MinecraftHelp<CommandSource> minecraftHelp;

    /// Constructs a new [HelpCommand] and eagerly initializes the
    /// [MinecraftHelp] renderer with the JIS Z 9103 color scheme.
    ///
    /// The [MinecraftHelp] instance is created once at injection time and
    /// reused for every command invocation. The `commandPrefix` is set to
    /// `"/template help"` so that click-through navigation in the help UI
    /// re-issues this command with the selected query string pre-filled.
    ///
    /// @param manager the Cloud command manager; used both to build the
    ///                command tree and to query the root index for
    ///                tab-completion suggestions
    @Inject
    private HelpCommand(
            final CommandManager<CommandSource> manager,
            final Messages messages
    ) {
        this.manager = manager;
        this.messages = messages;
        this.minecraftHelp = MinecraftHelp.<CommandSource>builder()
                .commandManager(manager)
                .audienceProvider(CommandSource::sender)
                .commandPrefix("/%s help".formatted(COMMAND_NAME))
                .colors(MinecraftHelp.helpColors(PRIMARY, HIGHLIGHT, ALT_HIGHLIGHT, TEXT, ACCENT))
                .messageProvider(new MessageProvider())
                .headerFooterLength(53)
                .build();
    }

    /// {@inheritDoc}
    ///
    /// Registers `/template help [query]` where `query` is an optional
    /// greedy-string argument that defaults to `""`.
    ///
    /// Omitting `query` renders the root index view; supplying a full
    /// command-syntax string renders the verbose detail view. The
    /// [SuggestionProvider] populates completions from
    /// [org.incendo.cloud.help.HelpHandler#queryRootIndex], which evaluates
    /// command permissions for the requesting sender at completion time.
    @Override
    public Command<CommandSource> command() {
        return this.manager.commandBuilder(COMMAND_NAME)
                .literal("help")
                .permission(TemplatePermission.COMMAND_HELP.node())
                .commandDescription(this.description())
                .optional("query",
                        StringParser.greedyStringParser(),
                        DefaultValue.constant(""),
                        new SuggestionProvider()
                )
                .handler(this::executes)
                .build();
    }

    private void executes(final CommandContext<CommandSource> context) {
        final String query = context.getOrDefault("query", "");
        this.minecraftHelp.queryCommands(query, context.sender());
    }

    private CommandDescription description() {
        return CommandDescription.commandDescription(RichDescription.of(this.messages.commandHelpDescription()));
    }

    private final class SuggestionProvider implements BlockingSuggestionProvider<CommandSource> {

        @Override
        public Iterable<? extends Suggestion> suggestions(final CommandContext<CommandSource> context, final CommandInput input) {
            return HelpCommand.this.manager.createHelpHandler()
                    .queryRootIndex(context.sender())
                    .entries()
                    .stream()
                    .map(CommandEntry::syntax)
                    .map(Suggestion::suggestion)
                    .toList();
        }
    }

    private final class MessageProvider implements MinecraftHelp.MessageProvider<CommandSource> {

        @Override
        public Component provide(final CommandSource sender, final String key, final Map<String, String> args) {
            final TagResolver.Builder builder = TagResolver.builder();
            args.forEach((k, value) -> builder.resolver(Placeholder.parsed(k, value)));
            final TagResolver placeholders = builder.build();

            return switch (key) {
                case "arguments" -> HelpCommand.this.messages.commandHelpMiscArguments(placeholders);
                case "available_commands" -> HelpCommand.this.messages.commandHelpMiscAvailableCommands(placeholders);
                case "click_for_next_page" -> HelpCommand.this.messages.commandHelpMiscClickForNextPage(placeholders);
                case "click_for_previous_page" -> HelpCommand.this.messages.commandHelpMiscClickForPreviousPage(placeholders);
                case "click_to_show_help" -> HelpCommand.this.messages.commandHelpMiscClickToShowHelp(placeholders);
                case "command" -> HelpCommand.this.messages.commandHelpMiscCommand(placeholders);
                case "description" -> HelpCommand.this.messages.commandHelpMiscDescription(placeholders);
                case "help" -> HelpCommand.this.messages.commandHelpMiscHelp(placeholders);
                case "no_description" -> HelpCommand.this.messages.commandHelpMiscNoDescription(placeholders);
                case "no_results_for_query" -> HelpCommand.this.messages.commandHelpMiscNoResultsForQuery(placeholders);
                case "optional" -> HelpCommand.this.messages.commandHelpMiscOptional(placeholders);
                case "page_out_of_range" -> HelpCommand.this.messages.commandHelpMiscPageOutOfRange(placeholders);
                case "showing_results_for_query" -> HelpCommand.this.messages.commandHelpMiscShowingResultsForQuery(placeholders);
                default -> throw new IllegalArgumentException();
            };
        }
    }
}

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
package io.github.namiuni.paperplugintemplate.common.command.commands;

import io.github.namiuni.paperplugintemplate.common.Metadata;
import io.github.namiuni.paperplugintemplate.common.command.CommandSource;
import io.github.namiuni.paperplugintemplate.common.infrastructure.configuration.configurations.PrimaryConfiguration;
import io.github.namiuni.paperplugintemplate.common.infrastructure.translation.translations.MessageAssembly;
import io.github.namiuni.paperplugintemplate.common.permission.PluginPermissions;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import net.kyori.adventure.pointer.Pointered;
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

@NullMarked
public final class HelpCommand implements CommandFactory {

    private final CommandManager<CommandSource> manager;
    private final MessageAssembly messages;
    private final Provider<PrimaryConfiguration> primaryConfig;
    private final Metadata metadata;

    @Inject
    HelpCommand(
            final CommandManager<CommandSource> manager,
            final MessageAssembly messages,
            final Provider<PrimaryConfiguration> primaryConfig,
            final Metadata metadata
    ) {
        this.manager = manager;
        this.messages = messages;
        this.primaryConfig = primaryConfig;
        this.metadata = metadata;
    }

    @Override
    public Command<CommandSource> createCommand() {
        final MinecraftHelp<CommandSource> minecraftHelp = this.buildHelp();
        return this.manager.commandBuilder(this.metadata.namespace())
                .literal("help")
                .permission(PluginPermissions.COMMAND_HELP)
                .commandDescription(this.description())
                .optional(
                        "query",
                        StringParser.greedyStringParser(),
                        DefaultValue.constant(""),
                        new SuggestionProvider()
                )
                .handler(context -> this.executes(context, minecraftHelp))
                .build();
    }

    private MinecraftHelp<CommandSource> buildHelp() {
        final PrimaryConfiguration.UI.Help colors = this.primaryConfig.get().ui().help();
        return MinecraftHelp.<CommandSource>builder()
                .commandManager(this.manager)
                .audienceProvider(CommandSource::sender)
                .commandPrefix("/%s help".formatted(this.metadata.namespace()))
                .colors(MinecraftHelp.helpColors(
                        parseColor(colors.primaryColor()),
                        parseColor(colors.highlightColor()),
                        parseColor(colors.altHighlightColor()),
                        parseColor(colors.textColor()),
                        parseColor(colors.accentColor())
                ))
                .messageProvider(new MessageProvider())
                .headerFooterLength(53)
                .build();
    }

    private void executes(final CommandContext<CommandSource> context, final MinecraftHelp<CommandSource> help) {
        final String query = context.getOrDefault("query", "");
        help.queryCommands(query, context.sender());
    }

    private CommandDescription description() {
        return CommandDescription.commandDescription(RichDescription.of(this.messages.commandHelpDescription()));
    }

    private static TextColor parseColor(final String hex) {
        return Objects.requireNonNull(
                TextColor.fromHexString(hex),
                "Invalid hex color in help configuration: '%s'. Expected format: #RRGGBB".formatted(hex)
        );
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

        private final Map<String, BiFunction<Pointered, TagResolver, Component>> resolvers = Map.ofEntries(
                Map.entry("arguments", HelpCommand.this.messages::commandHelpMiscArguments),
                Map.entry("available_commands", HelpCommand.this.messages::commandHelpMiscAvailableCommands),
                Map.entry("click_for_next_page", HelpCommand.this.messages::commandHelpMiscClickForNextPage),
                Map.entry("click_for_previous_page", HelpCommand.this.messages::commandHelpMiscClickForPreviousPage),
                Map.entry("click_to_show_help", HelpCommand.this.messages::commandHelpMiscClickToShowHelp),
                Map.entry("command", HelpCommand.this.messages::commandHelpMiscCommand),
                Map.entry("description", HelpCommand.this.messages::commandHelpMiscDescription),
                Map.entry("help", HelpCommand.this.messages::commandHelpMiscHelp),
                Map.entry("no_description", HelpCommand.this.messages::commandHelpMiscNoDescription),
                Map.entry("no_results_for_query", HelpCommand.this.messages::commandHelpMiscNoResultsForQuery),
                Map.entry("optional", HelpCommand.this.messages::commandHelpMiscOptional),
                Map.entry("page_out_of_range", HelpCommand.this.messages::commandHelpMiscPageOutOfRange),
                Map.entry("showing_results_for_query", HelpCommand.this.messages::commandHelpMiscShowingResultsForQuery)
        );

        @Override
        @SuppressWarnings("PatternValidation")
        public Component provide(final CommandSource sender, final String key, final Map<String, String> args) {
            final TagResolver placeholders = TagResolver.resolver(
                    args.entrySet().stream()
                            .map(entry -> Placeholder.parsed(entry.getKey(), entry.getValue()))
                            .toArray(TagResolver[]::new)
            );

            final BiFunction<Pointered, TagResolver, Component> resolver = this.resolvers.get(key);
            return resolver.apply(sender.sender(), placeholders);
        }
    }
}

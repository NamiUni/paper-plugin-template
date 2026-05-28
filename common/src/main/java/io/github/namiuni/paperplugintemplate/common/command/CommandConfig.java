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
package io.github.namiuni.paperplugintemplate.common.command;

import io.github.namiuni.paperplugintemplate.common.infrastructure.configuration.annotations.ConfigHeader;
import io.github.namiuni.paperplugintemplate.common.infrastructure.configuration.annotations.ConfigName;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import org.jspecify.annotations.NullMarked;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

@NullMarked
@ConfigSerializable
@ConfigName("command.conf")
@ConfigHeader("")
public record CommandConfig(Admin admin) {

    public static final CommandConfig DEFAULT = new CommandConfig(
            new CommandConfig.Admin(
                    Component.translatable("commands.template.description", ""), // TODO
                    List.of("template", "papertemplate", "plugintemplate"),
                    new CommandConfig.Admin.Reload(
                            Component.translatable("commands.template.reload.description", "Reloads plugin configuration."),
                            List.of(),
                            Map.ofEntries(
                                    Map.entry("success", Component.translatable("commands.template.reload.success", "Configuration reloaded successfully.")),
                                    Map.entry("failure", Component.translatable("commands.template.reload.failure", "Failed to reload configuration. See the console for details."))
                            )
                    ),
                    new CommandConfig.Admin.Help(
                            Component.translatable("commands.template.help.description", "Displays help for plugin commands."),
                            List.of(),
                            Map.ofEntries(
                                    Map.entry("arguments", Component.translatable("commands.template.help.arguments", "Arguments")),
                                    Map.entry("available_commands", Component.translatable("commands.template.help.available_commands", "Available Commands")),
                                    Map.entry("click_for_next_page", Component.translatable("commands.template.help.click_for_next_page", "Click for next page")),
                                    Map.entry("click_for_previous_page", Component.translatable("commands.template.help.click_for_previous_page", "Click for previous page")),
                                    Map.entry("click_to_show_help", Component.translatable("commands.template.help.click_to_show_help", "Click to show help for this command")),
                                    Map.entry("command", Component.translatable("commands.template.help.command", "Command")),
                                    Map.entry("description", Component.translatable("commands.template.help.description", "Description")),
                                    Map.entry("help", Component.translatable("commands.template.help.help", "Help")),
                                    Map.entry("no_description", Component.translatable("commands.template.help.no_description", "No Description")),
                                    Map.entry("no_results_for_query", Component.translatable("commands.template.help.no_results_for_query", "No results for query")),
                                    Map.entry("optional", Component.translatable("commands.template.help.optional", "Optional")),
                                    Map.entry("page_out_of_range", Component.translatable("commands.template.help.page_out_of_range", "Error: Page <page> is not in range. Must be in range [1, <max_pages>]")),
                                    Map.entry("showing_results_for_query", Component.translatable("commands.template.help.showing_results_for_query", "Showing search results for query"))
                            ),
                            new CommandConfig.Admin.Help.Colors(
                                    "#2D7D9A",
                                    "#49E1E8",
                                    "#E3008C",
                                    "#FFFFFF",
                                    "#7D7D7D"
                            )
                    )
            )
    );

    public sealed interface Node permits Admin, Admin.Reload, Admin.Help {

        Component description();

        List<String> aliases();

    }

    @ConfigSerializable
    public record Admin(
            Component description,
            List<String> aliases,
            Admin.Reload reload,
            Admin.Help help
    ) implements Node {

        @ConfigSerializable
        public record Reload(
                Component description,
                List<String> aliases,
                Map<String, Component> messages
        ) implements Node {
        }

        @ConfigSerializable
        public record Help(
                Component description,
                List<String> aliases,
                Map<String, Component> messages,
                @Comment("Colors used in the /help command output.")
                Admin.Help.Colors colors
        ) implements Node {

            @ConfigSerializable
            public record Colors(

                    @Comment("Primary color for section headers and command names. Hex format: #RRGGBB")
                    String primary,

                    @Comment("Highlight color for clickable elements and key terms. Hex format: #RRGGBB")
                    String highlight,

                    @Comment("Alternative highlight used for parameter hints and secondary emphasis. Hex format: #RRGGBB")
                    String altHighlight,

                    @Comment("Default body text color. Hex format: #RRGGBB")
                    String text,

                    @Comment("Accent color for decorative separators and less-prominent elements. Hex format: #RRGGBB")
                    String accent
            ) {
            }
        }
    }
}

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
package io.github.namiuni.paperplugintemplate.common.infrastructure.configuration.configurations;

import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import org.jspecify.annotations.NullMarked;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

@NullMarked
@ConfigSerializable
public record CommandConfiguration(Admin admin) {

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

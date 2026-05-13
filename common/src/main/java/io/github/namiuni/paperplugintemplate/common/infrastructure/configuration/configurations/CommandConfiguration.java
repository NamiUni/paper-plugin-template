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

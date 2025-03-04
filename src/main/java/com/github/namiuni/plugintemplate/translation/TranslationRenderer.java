/*
 * PluginTemplate
 *
 * Copyright (c) 2025. Namiu/Unitarou
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
package com.github.namiuni.plugintemplate.translation;

import com.github.namiuni.plugintemplate.configuration.ConfigurationManager;
import com.github.namiuni.plugintemplate.integration.MiniPlaceholdersExpansion;
import com.google.inject.Inject;
import io.github.miniplaceholders.api.MiniPlaceholders;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.moonshine.message.IMessageRenderer;
import org.intellij.lang.annotations.Subst;
import org.jspecify.annotations.NullMarked;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Map;

@NullMarked
public final class TranslationRenderer implements IMessageRenderer<Audience, String, Component, Component> {

    private final ConfigurationManager configManager;
    private final MiniMessage miniMessage;

    @Inject
    private TranslationRenderer(final ConfigurationManager configManager) {
        this.configManager = configManager;
        this.miniMessage = this.customMiniMessage();
    }

    @Override
    public Component render(
            final Audience receiver,
            final String intermediateMessage,
            final Map<String, ? extends Component> resolvedPlaceholders,
            final Method method,
            final Type owner
    ) {
        final TagResolver.Builder builder = TagResolver.builder();
        for (final Map.Entry<String, ? extends Component> entry : resolvedPlaceholders.entrySet()) {
            @Subst("message-key") final String key = entry.getKey();
            builder.resolver(Placeholder.component(key, entry.getValue()));
        }

        if (MiniPlaceholdersExpansion.miniPlaceholdersLoaded()) {
            builder.resolver(MiniPlaceholders.getGlobalPlaceholders())
                    .resolver(MiniPlaceholders.getAudienceGlobalPlaceholders(receiver));
        }

        return this.miniMessage.deserialize(intermediateMessage, builder.build());
    }

    private MiniMessage customMiniMessage() {
        final TagResolver.Builder tagResolver = TagResolver.builder()
                .resolver(TagResolver.standard());

        // JIS Z 9103 https://safetycolor.jp/shiteichi/
        final TextColor red = TextColor.color(Integer.parseInt("ff4b00", 16));
//        final TextColor orange = TextColor.color(Integer.parseInt("f6aa00", 16));
        final TextColor yellow = TextColor.color(Integer.parseInt("f2e700", 16));
        final TextColor green = TextColor.color(Integer.parseInt("00b06b", 16));
        final TextColor blue = TextColor.color(Integer.parseInt("1971ff", 16));
//        final var purple = TextColor.color(Integer.parseInt("990099", 16));

        tagResolver
                .tag("error", Tag.styling(red))
                .tag("warn", Tag.styling(yellow))
                .tag("info", Tag.styling(green))
                .tag("debug", Tag.styling(blue));

        final String prefix = this.configManager.primary().messagePrefix();
        if (!prefix.isBlank()) {
            tagResolver.resolver(Placeholder.parsed("prefix", prefix));
        }

        return MiniMessage.builder()
                .tags(tagResolver.build())
                .build();
    }
}

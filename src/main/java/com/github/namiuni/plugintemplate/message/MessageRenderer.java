/*
 * plugin-template
 *
 * Copyright (c) 2024. Namiu (Unitarou)
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

package com.github.namiuni.plugintemplate.message;

import com.github.namiuni.plugintemplate.integration.MiniPlaceholdersExpansion;
import io.github.miniplaceholders.api.MiniPlaceholders;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.moonshine.message.IMessageRenderer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.intellij.lang.annotations.Subst;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Map;

@DefaultQualifier(NonNull.class)
public final class MessageRenderer implements IMessageRenderer<Audience, String, Component, Component> {

    @Override
    public Component render(
            final Audience receiver,
            final String intermediateMessage,
            final Map<String, ? extends Component> resolvedPlaceholders,
            final Method method,
            final Type owner
    ) {
        final TagResolver.Builder builder = TagResolver.builder();
        for ( final var entry : resolvedPlaceholders.entrySet()) {
            @Subst("any-key") final var key = entry.getKey();
            builder.resolver(Placeholder.component(key, entry.getValue()));
        }

        if (MiniPlaceholdersExpansion.miniPlaceholdersEnabled()) {
            final var globalTag = MiniPlaceholders.getAudienceGlobalPlaceholders(receiver);
            final var audienceTag = MiniPlaceholders.getAudiencePlaceholders(receiver);
            builder.resolver(globalTag)
                    .resolver(audienceTag);
        }

        return MiniMessage.miniMessage().deserialize(intermediateMessage, builder.build());
    }
}

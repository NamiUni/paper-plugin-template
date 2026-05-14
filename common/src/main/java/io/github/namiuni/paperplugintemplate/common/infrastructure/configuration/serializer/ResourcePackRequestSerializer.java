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
package io.github.namiuni.paperplugintemplate.common.infrastructure.configuration.serializer;

import java.lang.reflect.Type;
import java.util.List;
import net.kyori.adventure.resource.ResourcePackInfo;
import net.kyori.adventure.resource.ResourcePackRequest;
import net.kyori.adventure.text.Component;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

@NullMarked
public final class ResourcePackRequestSerializer implements TypeSerializer<ResourcePackRequest> {

    public static final ResourcePackRequestSerializer INSTANCE = new ResourcePackRequestSerializer();

    private static final String PACKS = "packs";
    private static final String PROMPT = "prompt";
    private static final String REPLACE = "replace";
    private static final String REQUIRED = "required";

    private ResourcePackRequestSerializer() {
    }

    @Override
    public ResourcePackRequest deserialize(final Type type, final ConfigurationNode node) throws SerializationException {
        final List<ResourcePackInfo> info = node.node(PACKS).getList(ResourcePackInfo.class, List.of());
        final Component prompt = node.node(PROMPT).get(Component.class, Component.empty());
        final boolean replace = node.node(REPLACE).getBoolean();
        final boolean required = node.node(REQUIRED).getBoolean();

        return ResourcePackRequest.resourcePackRequest()
                .packs(info)
                .prompt(prompt)
                .replace(replace)
                .required(required)
                .build();
    }

    @Override
    public void serialize(final Type type, @Nullable final ResourcePackRequest packRequest, final ConfigurationNode node) throws SerializationException {
        if (packRequest != null) {
            node.node(PACKS).setList(ResourcePackInfo.class, packRequest.packs());
            node.node(PROMPT).set(packRequest.prompt());
            node.node(REPLACE).set(packRequest.replace());
            node.node(REQUIRED).set(packRequest.required());
        }
    }
}

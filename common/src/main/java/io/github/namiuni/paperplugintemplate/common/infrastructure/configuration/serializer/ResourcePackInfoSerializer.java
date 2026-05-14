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
import java.net.URI;
import java.util.Objects;
import java.util.UUID;
import net.kyori.adventure.resource.ResourcePackInfo;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

@NullMarked
public final class ResourcePackInfoSerializer implements TypeSerializer<ResourcePackInfo> {

    public static final ResourcePackInfoSerializer INSTANCE = new ResourcePackInfoSerializer();

    private static final String URI = "uri";
    private static final String HASH = "hash";
    private static final String ID = "id";

    private ResourcePackInfoSerializer() {
    }

    @Override
    public ResourcePackInfo deserialize(final Type type, final ConfigurationNode node) throws SerializationException {
        final URI uri = node.node(URI).get(URI.class);
        final String hash = node.node(HASH).getString();
        final UUID id = node.node(ID).get(UUID.class, UUID.randomUUID());

        final ResourcePackInfo.Builder builder = ResourcePackInfo.resourcePackInfo();

        builder.uri(Objects.requireNonNull(uri));
        if (hash != null) {
            builder.hash(hash);
        }
        if (id != null) {
            builder.id(id);
        }

        return builder.build();
    }

    @Override
    public void serialize(final Type type, @Nullable final ResourcePackInfo resourcePackInfo, final ConfigurationNode node) throws SerializationException {
        if (resourcePackInfo != null) {
            node.node(URI).set(resourcePackInfo.uri());
            node.node(HASH).set(resourcePackInfo.hash());
            node.node(ID).set(resourcePackInfo.id());
        }
    }
}

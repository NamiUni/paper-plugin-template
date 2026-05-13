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
        final UUID id = node.node(ID).get(UUID.class);

        return ResourcePackInfo.resourcePackInfo()
                .uri(Objects.requireNonNull(uri))
                .hash(Objects.requireNonNull(hash))
                .id(Objects.requireNonNull(id))
                .build();
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

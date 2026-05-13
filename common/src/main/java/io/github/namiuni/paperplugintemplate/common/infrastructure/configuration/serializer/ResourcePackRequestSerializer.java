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
        final Component prompt = node.node(PROMPT).get(Component.class);
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

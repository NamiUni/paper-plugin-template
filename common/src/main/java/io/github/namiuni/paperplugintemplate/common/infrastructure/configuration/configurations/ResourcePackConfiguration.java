package io.github.namiuni.paperplugintemplate.common.infrastructure.configuration.configurations;

import java.net.URI;
import net.kyori.adventure.resource.ResourcePackInfo;
import net.kyori.adventure.resource.ResourcePackRequest;
import net.kyori.adventure.text.Component;
import org.jspecify.annotations.NullMarked;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@NullMarked
@ConfigSerializable
public record ResourcePackConfiguration(boolean send, ResourcePackRequest resourcePackRequest) {

    public static final ResourcePackConfiguration DEFAULT = new ResourcePackConfiguration(
            true,
            ResourcePackRequest.resourcePackRequest()
                    .packs(ResourcePackInfo.resourcePackInfo()
                            .uri(URI.create("https://github.com/NamiUni/paper-plugin-template/releases/download/1.0.0/PaperPluginTemplate.zip"))
                            .hash("7f1ee13bec9ee51caf006296eded63c7b1cc144c")
                            .build()
                    )
                    .replace(false)
                    .required(false)
                    .prompt(Component.translatable(
                                    "resourcepacks.paperplugintemplate.question",
                                    "Please download the resource pack!"
                            )
                    )
                    .build()
    );
}

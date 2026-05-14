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

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import net.kyori.adventure.resource.ResourcePackInfo;
import net.kyori.adventure.resource.ResourcePackRequest;
import net.kyori.adventure.text.Component;
import org.jspecify.annotations.NullMarked;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

@NullMarked
@ConfigSerializable
public record ResourcePackConfiguration(

        @Comment("Whether to send the resource pack to players on join.")
        boolean send,

        @Comment("")
        ResourcePackRequest request
) {

    public static final ResourcePackConfiguration DEFAULT = new ResourcePackConfiguration(
            true,
            ResourcePackRequest.resourcePackRequest()
                    .packs(ResourcePackInfo.resourcePackInfo()
                            .uri(URI.create("https://github.com/NamiUni/paper-plugin-template/releases/download/1.0.0/PaperPluginTemplate.zip"))
                            .hash(loadBuildHash())
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

    private static final String BUILD_HASH_RESOURCE = "/resource-pack.sha1";

    private static String loadBuildHash() {
        try (var in = ResourcePackConfiguration.class.getResourceAsStream(BUILD_HASH_RESOURCE)) {
            return in != null ? readString(in) : "";
        } catch (final IOException _) {
            return "";
        }
    }

    private static String readString(final InputStream in) throws IOException {
        return new String(in.readAllBytes(), StandardCharsets.UTF_8).strip();
    }
}

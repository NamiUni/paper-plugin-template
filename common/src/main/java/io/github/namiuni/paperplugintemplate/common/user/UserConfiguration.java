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
package io.github.namiuni.paperplugintemplate.common.user;

import io.github.namiuni.paperplugintemplate.common.infrastructure.configuration.annotations.ConfigHeader;
import io.github.namiuni.paperplugintemplate.common.infrastructure.configuration.annotations.ConfigName;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import net.kyori.adventure.resource.ResourcePackInfo;
import net.kyori.adventure.resource.ResourcePackRequest;
import net.kyori.adventure.text.Component;
import org.jspecify.annotations.NullMarked;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

@NullMarked
@ConfigSerializable
@ConfigName("user.conf")
@ConfigHeader("")
public record UserConfiguration(

        @Comment("In-memory player-profile cache settings.")
        Cache cache,

        @Comment("Messages sent to players during user lifecycle events.")
        Messages messages,

        @Comment("")
        ResourcePack resourcePack
) {

    public static final UserConfiguration DEFAULT = new UserConfiguration(
            new Cache(
                    100L,
                    TimeUnit.MINUTES.toNanos(15L),
                    30L
            ),
            new Messages(
                    Component.translatable(
                            "connect.failure.load_user_profile",
                            "Failed to load your profile."
                    )
            ),
            new ResourcePack(
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
                                            "connect.resourcepack.paperplugintemplate",
                                            "Please download the resource pack!"
                                    )
                            )
                            .build()
            )
    );
    private static final String BUILD_HASH_RESOURCE = "/resource-pack.sha1";

    @ConfigSerializable
    public record Cache(

            @Comment("Maximum number of player entries held in the in-memory cache.")
            long maximumSize,

            @Comment("""
                    Duration in nanoseconds before an offline player's cache entry expires
                    after their last access. Does not affect online players.""")
            long expireAfterOffline,

            @Comment("""
                    Duration in seconds before a pre-login profile entry expires.
                    This cache bridges the gap between the async pre-connect phase
                    and the synchronous join phase. Increase if players are frequently
                    disconnected due to slow storage on high-latency servers.""")
            long preloadExpireSeconds
    ) {
    }

    @ConfigSerializable
    public record ResourcePack(

            @Comment("Whether to send the resource pack to players on join.")
            boolean send,

            @Comment("")
            ResourcePackRequest request
    ) {
    }

    @ConfigSerializable
    public record Messages(

            @Comment("Sent when a player's profile cannot be loaded during the pre-connect phase.")
            Component connectFailureLoadProfile
    ) {
    }

    private static String loadBuildHash() {
        try (var in = ResourcePack.class.getResourceAsStream(BUILD_HASH_RESOURCE)) {
            return in != null ? readString(in) : "";
        } catch (final IOException _) {
            return "";
        }
    }

    private static String readString(final InputStream in) throws IOException {
        return new String(in.readAllBytes(), StandardCharsets.UTF_8).strip();
    }
}

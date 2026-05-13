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

import java.util.concurrent.TimeUnit;
import net.kyori.adventure.text.Component;
import org.jspecify.annotations.NullMarked;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

@NullMarked
@ConfigSerializable
public record UserConfiguration(
        @Comment("In-memory player-profile cache settings.")
        Cache cache,
        @Comment("Messages sent to players during user lifecycle events.")
        Messages messages
) {

    public static final UserConfiguration DEFAULT = new UserConfiguration(
            new Cache(
                    100L,
                    TimeUnit.MINUTES.toNanos(15L),
                    30L
            ),
            new Messages(
                    Component.translatable(
                            "disconnect.loginFailedInfo",
                            Component.translatable(
                                    "disconnect.loginFailedInfo.user_profile_could_not_be_loaded",
                                    "Failed to load your profile."
                            )
                    )
            )
    );

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
    public record Messages(

            @Comment("Sent when a player's profile cannot be loaded during the pre-connect phase.")
            Component joinFailureLoadProfile
    ) {
    }
}

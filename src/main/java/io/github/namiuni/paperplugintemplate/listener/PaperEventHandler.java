/*
 * paper-plugin-template
 *
 * Copyright (c) 2026. Namiu/Unitarou
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
package io.github.namiuni.paperplugintemplate.listener;

import io.github.namiuni.paperplugintemplate.translation.Messages;
import io.github.namiuni.paperplugintemplate.user.UserService;
import io.papermc.paper.connection.PlayerConfigurationConnection;
import io.papermc.paper.event.connection.configuration.AsyncPlayerConnectionConfigureEvent;
import jakarta.inject.Inject;
import java.util.UUID;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jspecify.annotations.NullMarked;

/// Bukkit event listener that bridges Paper network events to [UserService].
///
/// ## Threading notes
///
/// [#onConnection] intentionally blocks the Paper network configuration thread
/// via [java.util.concurrent.CompletableFuture#join()] to guarantee that a
/// [io.github.namiuni.paperplugintemplate.user.storage.UserProfile] is available
/// before the play phase begins. This is the correct pattern for
/// [AsyncPlayerConnectionConfigureEvent]: the event fires on a dedicated async
/// configuration thread, not the main server thread, so blocking it is safe.
///
/// [#onLeft] uses [java.util.concurrent.CompletableFuture#whenComplete] so that
/// the cache eviction via [UserService#discardUser] is always performed, regardless
/// of whether the preceding persistence operation succeeded or failed.
@NullMarked
@SuppressWarnings("UnstableApiUsage")
public final class PaperEventHandler implements Listener {

    private final UserService userService;
    private final ComponentLogger logger;
    private final Messages messages;

    /// @param userService the service managing user data
    /// @param logger      the plugin logger used for error reporting
    @Inject
    private PaperEventHandler(
            final UserService userService,
            final ComponentLogger logger,
            final Messages messages
    ) {
        this.userService = userService;
        this.logger = logger;
        this.messages = messages;
    }

    /// Loads or creates the player's [io.github.namiuni.paperplugintemplate.user.storage.UserProfile]
    /// before the play phase starts.
    ///
    /// Blocks the configuration thread until the profile is available so that
    /// downstream play-phase handlers can always find it in the cache. If the
    /// storage backend fails, the player is disconnected to prevent them from
    /// joining in a broken state.
    ///
    /// @param event the connection-configuration event
    @EventHandler
    public void onConnection(final AsyncPlayerConnectionConfigureEvent event) {
        final PlayerConfigurationConnection connection = event.getConnection();
        final UUID uuid = connection.getProfile().getId();
        final String name = connection.getProfile().getName();
        if (uuid == null) {
            return;
        }

        this.userService.loadUser(uuid, name)
                .whenComplete((userProfile, ex) -> {
                    if (ex != null) {
                        this.logger.error("Failed to load user profile for UUID: {}. Disconnecting player.", uuid, ex);
                        connection.disconnect(this.messages.joinFailureProfile());
                    } else {
                        this.logger.debug(userProfile.toString());
                    }
                });
    }

    /// Persists the player's profile on disconnect, then evicts the cache entry.
    ///
    /// The eviction via [UserService#discardUser] runs unconditionally inside
    /// `whenComplete`, ensuring the cache does not retain stale entries even when
    /// the persistence operation fails. Failures are logged but not re-thrown.
    ///
    /// @param event the quit event
    @EventHandler
    public void onLeft(final PlayerQuitEvent event) {
        final UUID uuid = event.getPlayer().getUniqueId();
        this.userService.saveUser(uuid)
                .whenComplete((_, ex) -> {
                    if (ex != null) {
                        this.logger.error("Failed to persist profile on disconnect for UUID: {}", uuid, ex);
                    }
                    this.userService.discardUser(uuid);
                });
    }
}

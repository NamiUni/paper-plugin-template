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

import io.github.namiuni.paperplugintemplate.user.UserService;
import io.papermc.paper.connection.PlayerConfigurationConnection;
import io.papermc.paper.event.connection.configuration.AsyncPlayerConnectionConfigureEvent;
import jakarta.inject.Inject;
import java.util.UUID;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jspecify.annotations.NullMarked;

/// Bukkit event handler.
@NullMarked
@SuppressWarnings("UnstableApiUsage")
public final class PaperEventHandler implements Listener {

    private final UserService userService;

    /// Constructs a new `EventHandler`.
    ///
    /// @param userService the service managing user data and the in-memory cache
    @Inject
    private PaperEventHandler(final UserService userService) {
        this.userService = userService;
    }

    /// Starts a background cache warm-up for the connecting player.
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

        this.userService.getOrCreateUser(uuid, name).join(); // TODO: エラーハンドリング connection.disconnect();
    }

    @EventHandler
    public void onLeft(final PlayerQuitEvent event) {
        this.userService.getUser(event.getPlayer().getUniqueId())
                .thenAccept(user -> user.ifPresent(this.userService::upsertUser));
    }
}

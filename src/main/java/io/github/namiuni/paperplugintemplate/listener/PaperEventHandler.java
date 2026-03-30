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

/// Bukkit event listener that bridges Paper network events to [UserService].
///
/// ## Threading notes
///
/// [#onConnection] intentionally blocks the Paper network configuration thread
/// via [java.util.concurrent.CompletableFuture#join()] to guarantee that a
/// [io.github.namiuni.paperplugintemplate.user.storage.UserProfile] is available
/// before the play phase begins. This is the correct pattern for
/// [AsyncPlayerConnectionConfigureEvent]: the event fires on a dedicated async
/// thread, not the main server thread, so blocking it is safe.
@NullMarked
@SuppressWarnings("UnstableApiUsage")
public final class PaperEventHandler implements Listener {

    private final UserService userService;

    /// @param userService the service managing user data
    @Inject
    private PaperEventHandler(final UserService userService) {
        this.userService = userService;
    }

    /// Loads (or creates) the player's profile before the play phase starts.
    ///
    /// Blocks until the profile is available so that downstream handlers in the
    /// play phase can always find it in the repository.
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

        this.userService.loadUser(uuid, name).join(); // TODO: エラーハンドリング connection.disconnect();
    }

    /// Persists the player's profile on disconnect.
    ///
    /// The upsert is fire-and-forget; failures are not propagated to the event thread.
    ///
    /// @param event the quit event
    @EventHandler
    public void onLeft(final PlayerQuitEvent event) {
        final UUID uuid = event.getPlayer().getUniqueId();
        this.userService.saveUser(uuid)
                .thenRun(() -> this.userService.discardUser(uuid));
    }
}

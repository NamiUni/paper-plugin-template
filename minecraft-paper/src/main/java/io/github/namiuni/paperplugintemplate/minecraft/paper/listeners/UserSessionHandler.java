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
package io.github.namiuni.paperplugintemplate.minecraft.paper.listeners;

import io.github.namiuni.paperplugintemplate.common.infrastructure.translation.translations.MessageAssembly;
import io.github.namiuni.paperplugintemplate.common.user.UserServiceInternal;
import io.papermc.paper.event.connection.configuration.AsyncPlayerConnectionConfigureEvent;
import jakarta.inject.Inject;
import java.util.UUID;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.WorldSaveEvent;
import org.jspecify.annotations.NullMarked;

/// Bukkit event listener that drives the player user lifecycle for the plugin.
///
/// Bridges four Paper events to [UserServiceInternal]:
///
/// - [AsyncPlayerConnectionConfigureEvent] — triggers a profile preload during
///   the configuration phase so that the user's persistent record is warm in the
///   preload cache before the `Player` object is constructed. Disconnects the
///   client on repository failure.
/// - [PlayerJoinEvent] — constructs and caches the fully initialized
///   [io.github.namiuni.paperplugintemplate.api.user.PluginTemplateUser] once the
///   live `Player` object is available.
/// - [PlayerQuitEvent] — persists the user's current state to storage.
/// - [WorldSaveEvent] — persists the state of every online player on each
///   world-save cycle to bound data loss on unexpected shutdown.
///
/// ## Error handling
///
/// Profile-load failures during `onConnect` result in a graceful client disconnect
/// with a localized error message. Failures during `onJoin` are logged at ERROR
/// level; the player is allowed to remain online but may have a default profile.
///
/// ## Thread safety
///
/// `onConnect` executes on an async Paper network thread. `onJoin`, `onDisconnect`,
/// and `onWorldSave` execute on the main server thread. All delegate exclusively to
/// thread-safe methods on [UserServiceInternal].
@NullMarked
@SuppressWarnings({"UnstableApiUsage", "unused"})
public final class UserSessionHandler implements Listener {

    private final UserServiceInternal userService;
    private final ComponentLogger logger;
    private final MessageAssembly messages;

    /// Constructs a new event handler.
    ///
    /// @param userService the internal service managing in-memory and persistent user state
    /// @param logger      the component-aware logger used for structured error reporting
    /// @param messages    the localized message provider
    @Inject
    private UserSessionHandler(
            final UserServiceInternal userService,
            final ComponentLogger logger,
            final MessageAssembly messages
    ) {
        this.userService = userService;
        this.logger = logger;
        this.messages = messages;
    }

    // TODO: 失敗した場合にユーザーを切断するか否かの設定と、接続を続行する場合の動作
    @EventHandler(priority = EventPriority.MONITOR)
    private void onConnect(final AsyncPlayerConnectionConfigureEvent event) {
        final var connection = event.getConnection();
        if (!connection.isConnected()) {
            return;
        }

        final UUID uuid = connection.getProfile().getId();
        if (uuid == null) {
            return;
        }

        this.userService.loadUserRecord(
                uuid,
                () -> connection.disconnect(this.messages.joinFailureProfile(connection.getAudience()))
        );
    }

    // TODO: 失敗した場合にユーザーを切断するか否かの設定と、接続を続行する場合の動作
    @EventHandler(priority = EventPriority.MONITOR)
    private void onJoin(final PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        this.userService.loadUser(player)
                .whenComplete((_, exception) -> {
                    if (exception != null) {
                        this.logger.error("Failed to load user on join for UUID: {}", player.getUniqueId(), exception);
                    }
                });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onDisconnect(final PlayerQuitEvent event) {
        final Player player = event.getPlayer();
        final UUID uuid = player.getUniqueId();
        this.userService.saveUser(uuid);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onWorldSave(final WorldSaveEvent event) {
        event.getWorld().getPlayers().stream()
                .map(Player::getUniqueId)
                .forEach(this.userService::saveUser);
    }
}

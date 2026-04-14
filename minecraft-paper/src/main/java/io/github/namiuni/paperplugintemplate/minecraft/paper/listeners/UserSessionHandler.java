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

@NullMarked
@SuppressWarnings({"UnstableApiUsage", "unused"})
public final class UserSessionHandler implements Listener {

    private final UserServiceInternal userService;
    private final ComponentLogger logger;
    private final MessageAssembly messages;

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

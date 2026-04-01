/*
 * PaperPluginTemplate
 *
 * Copyright (c) 2026. Namiu (ãã«ããã)
 *                     Contributors
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

import io.github.namiuni.paperplugintemplate.common.translation.Messages;
import io.github.namiuni.paperplugintemplate.common.user.PluginTemplateUserServiceInternal;
import io.papermc.paper.event.connection.configuration.AsyncPlayerConnectionConfigureEvent;
import jakarta.inject.Inject;
import java.time.Instant;
import java.util.UUID;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.WorldSaveEvent;
import org.jspecify.annotations.NullMarked;

// TODO: Javadoc
@NullMarked
@SuppressWarnings("UnstableApiUsage")
public final class PaperEventHandler implements Listener {

    private final PluginTemplateUserServiceInternal userService;
    private final ComponentLogger logger;
    private final Messages messages;

    @Inject
    private PaperEventHandler(
            final PluginTemplateUserServiceInternal userService,
            final ComponentLogger logger,
            final Messages messages
    ) {
        this.userService = userService;
        this.logger = logger;
        this.messages = messages;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onConnection(final AsyncPlayerConnectionConfigureEvent event) {
        final var connection = event.getConnection();
        if (!connection.isConnected()) {
            return;
        }

        final UUID uuid = event.getConnection().getProfile().getId();
        if (uuid == null) {
            return;
        }

        this.userService.loadUserProfile(uuid)
                .whenComplete((profile, ex) -> {
                    if (ex != null) {
                        this.logger.error("Failed to load user profile for UUID: {}. connecting player.", uuid, ex);
                        connection.disconnect(this.messages.joinFailureProfile());
                    } else if (profile.isPresent()) {
                        profile.get().withLastSeen(Instant.now());
                        this.logger.debug("Connect: {}", profile);
                    }
                });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onDisconnect(final PlayerQuitEvent event) {
        final UUID uuid = event.getPlayer().getUniqueId();
        this.userService.loadUser(event.getPlayer())
                .thenCompose(user -> {
                    this.logger.debug("Disconnect: {}", user);
                    user.lastSeen(Instant.ofEpochMilli(event.getPlayer().getLastSeen()));
                    return this.userService.upsertUser(user);
                })
                .whenComplete((_, ex) -> {
                    if (ex != null) {
                        this.logger.error("Failed to persist profile on disconnect for UUID: {}", uuid, ex);
                    }
                    this.userService.discardUser(uuid);
                });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onWorldSave(final WorldSaveEvent event) {
        for (final Player player : event.getWorld().getPlayers()) {
            this.userService.loadUser(player)
                    .thenCompose(user -> {
                        this.logger.debug("World Save: {}", user);
                        user.lastSeen(Instant.ofEpochMilli(player.getLastSeen()));
                        return this.userService.upsertUser(user);
                    })
                    .whenComplete((_, ex) -> {
                        if (ex != null) {
                            this.logger.error("Failed to persist profile on world save for UUID: {}", player.getUniqueId(), ex);
                        }
                    });
        }
    }
}

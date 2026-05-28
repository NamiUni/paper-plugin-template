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
package io.github.namiuni.paperplugintemplate.minecraft.paper.user;

import io.github.namiuni.paperplugintemplate.common.event.EventBus;
import io.github.namiuni.paperplugintemplate.common.event.events.PlayerConnectEvent;
import io.github.namiuni.paperplugintemplate.common.event.events.PlayerDisconnectEvent;
import io.github.namiuni.paperplugintemplate.common.event.events.PlayerPreConnectEvent;
import io.github.namiuni.paperplugintemplate.common.event.events.WorldCheckPointEvent;
import io.papermc.paper.event.connection.configuration.AsyncPlayerConnectionConfigureEvent;
import jakarta.inject.Inject;
import java.util.List;
import net.kyori.adventure.audience.Audience;
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
public final class UserSessionAdapter implements Listener {

    private final EventBus eventBus;

    @Inject
    UserSessionAdapter(final EventBus eventBus) {
        this.eventBus = eventBus;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    private void onPreConnect(final AsyncPlayerConnectionConfigureEvent event) {
        final var connection = event.getConnection();
        final Audience audience = connection.getAudience();

        this.eventBus.publish(new PlayerPreConnectEvent(audience, connection::disconnect));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onJoin(final PlayerJoinEvent event) {
        this.eventBus.publish(new PlayerConnectEvent(event.getPlayer()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onQuit(final PlayerQuitEvent event) {
        this.eventBus.publish(new PlayerDisconnectEvent(event.getPlayer()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onWorldSave(final WorldSaveEvent event) {
        final List<Player> players = event.getWorld().getPlayers();
        this.eventBus.publish(new WorldCheckPointEvent(players));
    }
}

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
package io.github.namiuni.paperplugintemplate.minecraft.sponge.user;

import io.github.namiuni.paperplugintemplate.common.event.EventBus;
import io.github.namiuni.paperplugintemplate.common.event.events.PlayerConnectEvent;
import io.github.namiuni.paperplugintemplate.common.event.events.PlayerDisconnectEvent;
import io.github.namiuni.paperplugintemplate.common.event.events.PlayerPreConnectEvent;
import io.github.namiuni.paperplugintemplate.common.event.events.WorldCheckPointEvent;
import jakarta.inject.Inject;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import net.kyori.adventure.audience.Audience;
import org.jspecify.annotations.NullMarked;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.network.ServerSideConnectionEvent;
import org.spongepowered.api.event.world.SaveWorldEvent;

@NullMarked
@SuppressWarnings("unused")
public final class UserSessionAdapter {

    private final EventBus eventBus;

    @Inject
    private UserSessionAdapter(final EventBus eventBus) {
        this.eventBus = eventBus;
    }

    @Listener(order = Order.POST)
    public void onLogin(final ServerSideConnectionEvent.Login event) {
        final UUID uuid = event.user().uniqueId();
        final Audience audience = event.cause().first(Audience.class).orElse(Audience.empty());
        this.eventBus.publish(new PlayerPreConnectEvent(
                uuid,
                audience,
                reason -> {
                    event.setMessage(reason);
                    event.setCancelled(true);
                }
        ));
    }

    @Listener(order = Order.POST)
    public void onJoin(final ServerSideConnectionEvent.Join event) {
        this.eventBus.publish(new PlayerConnectEvent<>(event.player()));
    }

    @Listener(order = Order.POST)
    public void onDisconnect(final ServerSideConnectionEvent.Disconnect event) {
        event.profile().ifPresent(profile ->
                this.eventBus.publish(new PlayerDisconnectEvent(profile.uniqueId())));
    }

    @Listener(order = Order.POST)
    public void onSave(final SaveWorldEvent event) {
        final Set<UUID> uuids = Sponge.server().onlinePlayers().stream()
                .map(ServerPlayer::uniqueId)
                .collect(Collectors.toUnmodifiableSet());
        this.eventBus.publish(new WorldCheckPointEvent(uuids));
    }
}

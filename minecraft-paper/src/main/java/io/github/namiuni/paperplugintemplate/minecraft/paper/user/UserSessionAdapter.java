package io.github.namiuni.paperplugintemplate.minecraft.paper.user;

import io.github.namiuni.paperplugintemplate.common.event.EventBus;
import io.github.namiuni.paperplugintemplate.common.event.events.PlayerConnectEvent;
import io.github.namiuni.paperplugintemplate.common.event.events.PlayerDisconnectEvent;
import io.github.namiuni.paperplugintemplate.common.event.events.PlayerPreConnectEvent;
import io.github.namiuni.paperplugintemplate.common.event.events.WorldCheckPointEvent;
import io.papermc.paper.event.connection.configuration.AsyncPlayerConnectionConfigureEvent;
import jakarta.inject.Inject;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
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
    private UserSessionAdapter(final EventBus eventBus) {
        this.eventBus = eventBus;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onPreConnect(final AsyncPlayerConnectionConfigureEvent event) {
        final var connection = event.getConnection();
        if (!connection.isConnected()) {
            return;
        }
        final UUID uuid = connection.getProfile().getId();
        if (uuid == null) {
            return;
        }

        this.eventBus.publish(new PlayerPreConnectEvent(uuid, connection.getAudience(), connection::disconnect));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onJoin(final PlayerJoinEvent event) {
        this.eventBus.publish(new PlayerConnectEvent<>(event.getPlayer()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onQuit(final PlayerQuitEvent event) {
        this.eventBus.publish(new PlayerDisconnectEvent(event.getPlayer().getUniqueId()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onWorldSave(final WorldSaveEvent event) {
        final Set<UUID> uuids = event.getWorld().getPlayers().stream()
                .map(Player::getUniqueId)
                .collect(Collectors.toUnmodifiableSet());
        this.eventBus.publish(new WorldCheckPointEvent(uuids));
    }
}

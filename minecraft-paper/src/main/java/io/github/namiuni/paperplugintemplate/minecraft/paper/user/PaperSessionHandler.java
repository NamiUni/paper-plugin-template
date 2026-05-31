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

import io.github.namiuni.paperplugintemplate.api.user.PluginTemplateUser;
import io.github.namiuni.paperplugintemplate.common.Metadata;
import io.github.namiuni.paperplugintemplate.common.infrastructure.configuration.ConfigHolder;
import io.github.namiuni.paperplugintemplate.common.user.PluginTemplateUserImpl;
import io.github.namiuni.paperplugintemplate.common.user.UserConfig;
import io.github.namiuni.paperplugintemplate.common.user.UserPointers;
import io.github.namiuni.paperplugintemplate.common.user.UserService;
import io.github.namiuni.paperplugintemplate.common.user.UserSettingImpl;
import io.github.namiuni.paperplugintemplate.common.user.storage.UserRecord;
import io.github.namiuni.paperplugintemplate.common.user.storage.UserRepository;
import io.papermc.paper.event.connection.configuration.AsyncPlayerConnectionConfigureEvent;
import jakarta.inject.Inject;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
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
@SuppressWarnings("UnstableApiUsage")
public final class PaperSessionHandler implements Listener {

    private final UserRepository repository;
    private final UserService userService;
    private final String pluginName;
    private final ComponentLogger logger;
    private final ConfigHolder<UserConfig> userConfig;

    @Inject
    PaperSessionHandler(
            final UserRepository repository,
            final UserService userService,
            final Metadata metadata,
            final ComponentLogger logger,
            final ConfigHolder<UserConfig> userConfig
    ) {
        this.repository = repository;
        this.userService = userService;
        this.pluginName = metadata.name();
        this.logger = logger;
        this.userConfig = userConfig;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onPreConnect(final AsyncPlayerConnectionConfigureEvent event) {
        final var connection = event.getConnection();
        if (!connection.isConnected()) {
            return;
        }

        final var uuid = event.getConnection().getProfile().getId();
        if (uuid == null) {
            return;
        }

        final Optional<UserRecord> userRecord;
        try {
            userRecord = this.repository.findById(uuid);
        } catch (final Exception exception) {
            this.logger.error("Failed to load user", exception);
            connection.disconnect(this.userConfig.get().messages().connectFailureLoadProfile());
            return;
        }

        final var audience = connection.getAudience();
        final var pointers = audience.pointers().toBuilder()
                .withStatic(UserPointers.AUDIENCE, audience)
                .withStatic(UserPointers.LAST_SEEN, userRecord
                        .map(UserRecord::lastSeen)
                        .orElse(Instant.ofEpochMilli(0))
                )
                .withStatic(UserPointers.SETTING, userRecord
                        .map(UserRecord::setting)
                        .orElse(UserSettingImpl.DEFAULT)
                )
                .build();
        final var user = new PluginTemplateUserImpl(pointers);
        this.userService.putUser(user);
        try {
            this.repository.upsert(UserRecord.from(user));
        } catch (final Exception exception) {
            this.logger.error("Failed to save user {}", uuid, exception);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onConnect(final PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        final UUID uuid = player.getUniqueId();

        Thread.ofVirtual().name(this.pluginName + "-User-Connect-" + uuid, 0)
                .start(() -> {
                    final var user = new PluginTemplateUserImpl(
                            player.pointers().toBuilder()
                                    .withStatic(UserPointers.AUDIENCE, player)
                                    .withDynamic(UserPointers.LAST_SEEN, () -> Instant.ofEpochMilli(player.getLastSeen()))
                                    .withDynamic(UserPointers.SETTING, () -> this.userService.getUser(uuid)
                                            .map(PluginTemplateUser::setting)
                                            .orElse(UserSettingImpl.DEFAULT))
                                    .build()
                    );
                    this.userService.putUser(user);

                    try {
                        this.repository.upsert(UserRecord.from(user));
                    } catch (final Exception exception) {
                        this.logger.error("Failed to save user {}", uuid, exception);
                    }
                });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onDisconnect(final PlayerQuitEvent event) {
        final Player player = event.getPlayer();
        final UUID uuid = player.getUniqueId();

        this.userService.getUser(uuid)
                .map(UserRecord::from)
                .ifPresent(snapshot -> Thread.ofVirtual().name(this.pluginName + "-User-Save-" + uuid, 0)
                        .start(() -> {
                            try {
                                this.repository.upsert(snapshot);
                            } catch (final Exception exception) {
                                this.logger.error("Failed to save user {}", uuid, exception);
                            }
                        }));

        this.userService.removeUser(uuid);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onWorldSave(final WorldSaveEvent event) {
        final List<UserRecord> snapshots = event.getWorld().getPlayers().stream()
                .flatMap(player -> this.userService.getUser(player.getUniqueId()).stream())
                .map(UserRecord::from)
                .toList();

        if (snapshots.isEmpty()) {
            return;
        }

        snapshots.forEach(snapshot -> Thread.ofVirtual().name(this.pluginName + "-User-Checkpoint-" + snapshot.name(), 0)
                .start(() -> {
                    try {
                        this.repository.upsert(snapshot);
                    } catch (final Exception exception) {
                        this.logger.error("Failed to checkpoint user {}", snapshot, exception);
                    }
                }));
    }
}

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
package io.github.namiuni.paperplugintemplate.common.user;

import io.github.namiuni.paperplugintemplate.common.event.EventBus;
import io.github.namiuni.paperplugintemplate.common.event.events.PlayerConnectEvent;
import io.github.namiuni.paperplugintemplate.common.event.events.PlayerDisconnectEvent;
import io.github.namiuni.paperplugintemplate.common.event.events.PlayerPreConnectEvent;
import io.github.namiuni.paperplugintemplate.common.event.events.WorldCheckPointEvent;
import io.github.namiuni.paperplugintemplate.common.user.storage.UserRecord;
import io.github.namiuni.paperplugintemplate.common.user.storage.UserRepository;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import java.util.UUID;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.jspecify.annotations.NullMarked;

@Singleton
@NullMarked
public final class UserSessionHandler {

    private final UserRepository repository;
    private final UserServiceInternal userService;
    private final Provider<UserConfiguration> userConfig;
    private final ComponentLogger logger;

    @Inject
    UserSessionHandler(
            final UserRepository repository,
            final UserServiceInternal userService,
            final Provider<UserConfiguration> userConfig,
            final ComponentLogger logger,
            final EventBus eventBus
    ) {
        this.repository = repository;
        this.userService = userService;
        this.userConfig = userConfig;
        this.logger = logger;

        eventBus.subscribe(PlayerPreConnectEvent.class, this::onPreConnect);
        eventBus.subscribe(PlayerConnectEvent.class, this::onConnect);
        eventBus.subscribe(PlayerDisconnectEvent.class, this::onDisconnect);
        eventBus.subscribe(WorldCheckPointEvent.class, this::onWorldCheckpoint);
    }

    private void onPreConnect(final PlayerPreConnectEvent event) {
        final Audience audience = event.audience();
        this.userService.loadUserOrCreate(audience)
                .whenComplete((_, exception) -> {
                    if (exception != null) {
                        final UUID uuid = audience.get(Identity.UUID).orElseThrow();
                        this.logger.error("Failed to pre-load profile for UUID: {}; disconnecting.", uuid, exception);
                        event.disconnector().disconnect(this.userConfig.get().messages().connectFailureLoadProfile());
                    }
                });
    }

    private void onConnect(final PlayerConnectEvent event) {
        final Audience audience = event.audience();
        this.userService.loadUserOrCreate(audience)
                .whenComplete((_, exception) -> {
                    if (exception != null) {
                        final UUID uuid = audience.get(Identity.UUID).orElseThrow();
                        this.logger.error("Failed to load player on join for UUID: {}", uuid, exception);
                    }
                })
                .thenAccept(user -> {
                    final UserConfiguration.ResourcePack packConfig = this.userConfig.get().resourcePack();
                    user.sendResourcePacks(packConfig.request());
                });
    }

    private void onDisconnect(final PlayerDisconnectEvent event) {
        this.userService.loadUserOrCreate(event.audience())
                .thenAccept(user -> {
                    final UserRecord record = new UserRecord(user.uuid(), user.name(), user.lastSeen());
                    this.repository.upsert(record);
                });
    }

    private void onWorldCheckpoint(final WorldCheckPointEvent event) {
        event.onlinePlayers()
                .forEach(audience -> this.userService.loadUserOrCreate(audience)
                        .thenAccept(user -> {
                            final UserRecord record = new UserRecord(user.uuid(), user.name(), user.lastSeen());
                            this.repository.upsert(record);
                        }));
    }
}

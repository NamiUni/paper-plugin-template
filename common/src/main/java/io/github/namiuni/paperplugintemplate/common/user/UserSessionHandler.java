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

import io.github.namiuni.paperplugintemplate.api.user.PluginTemplateUserService;
import io.github.namiuni.paperplugintemplate.common.event.EventBus;
import io.github.namiuni.paperplugintemplate.common.event.events.PlayerConnectEvent;
import io.github.namiuni.paperplugintemplate.common.event.events.PlayerDisconnectEvent;
import io.github.namiuni.paperplugintemplate.common.event.events.PlayerPreConnectEvent;
import io.github.namiuni.paperplugintemplate.common.event.events.WorldCheckPointEvent;
import io.github.namiuni.paperplugintemplate.common.infrastructure.translation.translations.MessageAssembly;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.jspecify.annotations.NullMarked;

@Singleton
@NullMarked
public final class UserSessionHandler {

    private final UserPersistenceCoordinator persistenceCoordinator;
    private final PluginTemplateUserService userService;
    private final MessageAssembly messages;
    private final ComponentLogger logger;

    @Inject
    UserSessionHandler(
            final UserPersistenceCoordinator persistenceCoordinator,
            final PluginTemplateUserService userService,
            final MessageAssembly messages,
            final ComponentLogger logger,
            final EventBus eventBus
    ) {
        this.persistenceCoordinator = persistenceCoordinator;
        this.userService = userService;
        this.messages = messages;
        this.logger = logger;

        eventBus.subscribe(PlayerPreConnectEvent.class, this::onPreConnect);
        eventBus.subscribe(PlayerConnectEvent.class, this::onConnect);
        eventBus.subscribe(PlayerDisconnectEvent.class, this::onDisconnect);
        eventBus.subscribe(WorldCheckPointEvent.class, this::onWorldCheckpoint);
    }

    private void onPreConnect(final PlayerPreConnectEvent event) {
        this.persistenceCoordinator.preload(
                event.uuid(),
                () -> event.disconnector().disconnect(this.messages.joinFailureProfile(event.audience()))
        );
    }

    private void onConnect(final PlayerConnectEvent<?> event) {
        this.userService.loadUser(event.player())
                .whenComplete((_, exception) -> {
                    if (exception != null) {
                        this.logger.error(
                                "Failed to load player on join for UUID: {}",
                                event.player().get(Identity.UUID).orElseThrow(),
                                exception
                        );
                    }
                });
    }

    private void onDisconnect(final PlayerDisconnectEvent event) {
        this.persistenceCoordinator.save(event.uuid());
    }

    private void onWorldCheckpoint(final WorldCheckPointEvent event) {
        event.onlinePlayerUuids().forEach(this.persistenceCoordinator::save);
    }
}

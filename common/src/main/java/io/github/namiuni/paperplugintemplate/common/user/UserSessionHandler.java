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
import io.github.namiuni.paperplugintemplate.common.infrastructure.storage.UserRecord;
import io.github.namiuni.paperplugintemplate.common.infrastructure.storage.UserRepository;
import io.github.namiuni.paperplugintemplate.common.infrastructure.translation.translations.MessageAssembly;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.jspecify.annotations.NullMarked;

@Singleton
@NullMarked
public final class UserSessionHandler {

    private final UserCache cache;
    private final UserRepository repository;
    private final PluginTemplateUserService userService;
    private final MessageAssembly messages;
    private final ComponentLogger logger;

    @Inject
    UserSessionHandler(
            final UserCache cache,
            final UserRepository repository,
            final PluginTemplateUserService userService,
            final MessageAssembly messages,
            final ComponentLogger logger,
            final EventBus eventBus
    ) {
        this.cache = cache;
        this.repository = repository;
        this.userService = userService;
        this.messages = messages;
        this.logger = logger;

        eventBus.subscribe(PlayerPreConnectEvent.class, this::onPreConnect);
        eventBus.subscribe(PlayerConnectEvent.class, this::onConnect);
        eventBus.subscribe(PlayerDisconnectEvent.class, this::onDisconnect);
        eventBus.subscribe(WorldCheckPointEvent.class, this::onWorldCheckpoint);
    }

    private void onPreConnect(final PlayerPreConnectEvent event) {
        this.preload(
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
        this.save(event.uuid());
    }

    private void onWorldCheckpoint(final WorldCheckPointEvent event) {
        event.onlinePlayerUuids().forEach(this::save);
    }

    private CompletableFuture<Void> preload(final UUID uuid, final Runnable disconnect) {
        if (this.cache.getUser(uuid).isPresent()) {
            this.logger.debug("[{}] userCache hit for {} — skipping repository.", UserSessionHandler.class.getSimpleName(), uuid);
            return CompletableFuture.completedFuture(null);
        }

        if (this.cache.getPreloaded(uuid).isPresent()) {
            this.logger.debug("[{}] preloadCache hit for {}.", UserSessionHandler.class.getSimpleName(), uuid);
            return CompletableFuture.completedFuture(null);
        }

        this.logger.debug("[{}] Cold miss for {} — querying repository.", UserSessionHandler.class.getSimpleName(), uuid);
        return this.repository.findById(uuid)
                .thenAccept(existing -> existing.ifPresent(record -> {
                    this.cache.cachePreloaded(uuid, record);
                    this.logger.debug("[{}] Profile stored in preloadCache for {}.", UserSessionHandler.class.getSimpleName(), uuid);
                }))
                .whenComplete((_, exception) -> {
                    if (exception != null) {
                        this.logger.error("Failed to pre-load profile for UUID: {}; disconnecting.", uuid, exception);
                        disconnect.run();
                    }
                });
    }

    private CompletableFuture<Void> save(final UUID uuid) {
        return this.cache.getUser(uuid)
                .map(user -> {
                    final UserRecord record = new UserRecord(user.uuid(), user.name(), Instant.now());
                    return this.repository.upsert(record)
                            .whenComplete((_, exception) -> {
                                if (exception != null) {
                                    this.logger.error("Failed to save player record for UUID: {}", uuid, exception);
                                }
                            });
                })
                .orElseGet(() -> CompletableFuture.completedFuture(null));
    }
}

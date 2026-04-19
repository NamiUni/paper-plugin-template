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

import io.github.namiuni.paperplugintemplate.common.infrastructure.storage.UserRecord;
import io.github.namiuni.paperplugintemplate.common.infrastructure.storage.UserRepository;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.jspecify.annotations.NullMarked;

@Singleton
@NullMarked
public final class UserPersistenceCoordinator {

    private final UserCache cache;
    private final UserRepository repository;
    private final Clock clock;
    private final ComponentLogger logger;

    @Inject
    UserPersistenceCoordinator(
            final UserCache cache,
            final UserRepository repository,
            final Clock clock,
            final ComponentLogger logger
    ) {
        this.cache = cache;
        this.repository = repository;
        this.clock = clock;
        this.logger = logger;
    }

    public CompletableFuture<Void> preload(final UUID uuid, final Runnable onFailure) {
        if (this.cache.getUser(uuid).isPresent()) {
            this.logger.debug("[{}] userCache hit for {} — skipping repository.", UserPersistenceCoordinator.class.getSimpleName(), uuid);
            return CompletableFuture.completedFuture(null);
        }

        if (this.cache.getPreloaded(uuid).isPresent()) {
            this.logger.debug("[{}] preloadCache hit for {}.", UserPersistenceCoordinator.class.getSimpleName(), uuid);
            return CompletableFuture.completedFuture(null);
        }

        this.logger.debug("[{}] Cold miss for {} — querying repository.", UserPersistenceCoordinator.class.getSimpleName(), uuid);
        return this.repository.findById(uuid)
                .thenAccept(existing -> existing.ifPresent(record -> {
                    this.cache.cachePreloaded(uuid, record);
                    this.logger.debug("[{}] Profile stored in preloadCache for {}.", UserPersistenceCoordinator.class.getSimpleName(), uuid);
                }))
                .whenComplete((_, exception) -> {
                    if (exception != null) {
                        this.logger.error("Failed to pre-load profile for UUID: {}; disconnecting.", uuid, exception);
                        onFailure.run();
                    }
                });
    }

    public CompletableFuture<Void> save(final UUID uuid) {
        return this.cache.getUser(uuid)
                .map(user -> {
                    final UserRecord record = new UserRecord(user.uuid(), user.name(), Instant.now(this.clock));
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

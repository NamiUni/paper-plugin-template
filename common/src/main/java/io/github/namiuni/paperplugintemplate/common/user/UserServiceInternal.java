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

import io.github.namiuni.paperplugintemplate.api.user.PluginTemplateUser;
import io.github.namiuni.paperplugintemplate.api.user.PluginTemplateUserService;
import io.github.namiuni.paperplugintemplate.common.infrastructure.storage.UserRecord;
import io.github.namiuni.paperplugintemplate.common.infrastructure.storage.UserRepository;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.identity.Identified;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.jspecify.annotations.NullMarked;

@Singleton
@NullMarked
public final class UserServiceInternal implements PluginTemplateUserService {

    private final UserCache cache;
    private final UserRepository repository;
    private final UserFactory userFactory;
    private final ComponentLogger logger;

    @Inject
    UserServiceInternal(
            final UserCache cache,
            final UserRepository repository,
            final UserFactory userFactory,
            final ComponentLogger logger
    ) {
        this.cache = cache;
        this.repository = repository;
        this.userFactory = userFactory;
        this.logger = logger;
    }

    @Override
    public Optional<PluginTemplateUser> getUser(final UUID uuid) {
        return this.cache.getUser(uuid);
    }

    @Override
    public <P extends Audience & Identified> CompletableFuture<PluginTemplateUser> loadUser(final P player) {
        final UUID uuid = player.get(Identity.UUID)
                .orElseThrow(() -> new IllegalArgumentException("Player is missing UUID identity: " + player.getClass().getName()));
        final String currentName = player.get(Identity.NAME)
                .orElseThrow(() -> new IllegalArgumentException("Player is missing NAME identity: " + player.getClass().getName()));

        final Optional<PluginTemplateUser> cached = this.cache.getUser(uuid);
        if (cached.isPresent()) {
            this.logger.debug("[{}] Tier-1 (userCache) hit for {}.", UserServiceInternal.class.getSimpleName(), uuid);
            return CompletableFuture.completedFuture(cached.get());
        }

        final Optional<UserRecord> preloaded = this.cache.getPreloaded(uuid);
        if (preloaded.isPresent()) {
            this.logger.debug("[{}] Tier-2 (preloadCache) hit for {}.", UserServiceInternal.class.getSimpleName(), uuid);
            final PluginTemplateUser user = this.userFactory.createUser(player, preloaded.get());
            this.cache.cacheUser(uuid, user);
            return CompletableFuture.completedFuture(user);
        }

        this.logger.debug("[{}] Tier-3 (repository) miss for {} — querying storage.", UserServiceInternal.class.getSimpleName(), uuid);
        return this.repository.findById(uuid)
                .thenApply(existing -> existing.orElseGet(() -> new UserRecord(uuid, currentName, Instant.now())))
                .thenApply(record -> {
                    final PluginTemplateUser user = this.userFactory.createUser(player, record);
                    this.cache.cacheUser(uuid, user);
                    return user;
                });
    }

    @Override
    public CompletableFuture<Void> deleteUser(final UUID uuid) {
        this.logger.debug("[{}] Removing all data for {}.", UserServiceInternal.class.getSimpleName(), uuid);
        this.cache.invalidate(uuid);
        return this.repository.delete(uuid);
    }
}

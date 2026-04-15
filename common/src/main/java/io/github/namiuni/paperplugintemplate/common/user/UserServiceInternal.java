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

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import io.github.namiuni.paperplugintemplate.api.user.PluginTemplateUser;
import io.github.namiuni.paperplugintemplate.api.user.PluginTemplateUserService;
import io.github.namiuni.paperplugintemplate.common.event.EventBus;
import io.github.namiuni.paperplugintemplate.common.event.events.PlayerConnectEvent;
import io.github.namiuni.paperplugintemplate.common.event.events.PlayerDisconnectEvent;
import io.github.namiuni.paperplugintemplate.common.event.events.PlayerPreConnectEvent;
import io.github.namiuni.paperplugintemplate.common.event.events.WorldCheckPointEvent;
import io.github.namiuni.paperplugintemplate.common.infrastructure.configuration.configurations.PrimaryConfiguration;
import io.github.namiuni.paperplugintemplate.common.infrastructure.persistence.UserRecord;
import io.github.namiuni.paperplugintemplate.common.infrastructure.persistence.UserRepository;
import io.github.namiuni.paperplugintemplate.common.infrastructure.translation.translations.MessageAssembly;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.identity.Identified;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.jspecify.annotations.NullMarked;

@Singleton
@NullMarked
public final class UserServiceInternal implements PluginTemplateUserService {

    private final UserRepository repository;
    private final UserFactory userFactory;
    private final ComponentLogger logger;

    private final Cache<UUID, UserRecord> preloadCache;
    private final Cache<UUID, PluginTemplateUser> userCache;

    @Inject
    private UserServiceInternal(
            final UserRepository repository,
            final UserFactory userFactory,
            final Provider<PrimaryConfiguration> primaryConfig,
            final ComponentLogger logger,
            final EventBus eventBus,
            final MessageAssembly messages
    ) {
        this.repository = repository;
        this.userFactory = userFactory;
        this.logger = logger;

        final PrimaryConfiguration.Storage.Cache cacheSettings = primaryConfig.get().storage().userCache();
        this.preloadCache = Caffeine.newBuilder()
                .expireAfterWrite(30L, TimeUnit.SECONDS)
                .build();
        this.userCache = Caffeine.newBuilder()
                .maximumSize(cacheSettings.maximumSize())
                .expireAfter(new OnlineAwareExpiry(cacheSettings))
                .build();

        eventBus.subscribe(PlayerPreConnectEvent.class, event ->
                this.preloadUserRecord(
                        event.uuid(),
                        () -> event.disconnector().disconnect(messages.joinFailureProfile(event.audience()))
                ));

        eventBus.subscribe(PlayerConnectEvent.class, event ->
                this.loadUser((Audience & Identified) event.player())
                        .whenComplete((_, exception) -> {
                            if (exception != null) {
                                logger.error(
                                        "Failed to load player on join for UUID: {}",
                                        event.player().get(Identity.UUID).orElseThrow(),
                                        exception
                                );
                            }
                        }));

        eventBus.subscribe(PlayerDisconnectEvent.class, event -> this.saveUser(event.uuid()));

        eventBus.subscribe(WorldCheckPointEvent.class, event -> event.onlinePlayerUuids().forEach(this::saveUser));
    }

    public CompletableFuture<Void> preloadUserRecord(final UUID uuid, final Runnable disconnect) {
        final PluginTemplateUser cachedUser = this.userCache.getIfPresent(uuid);
        if (cachedUser != null) {
            this.logger.debug("[{}] userCache hit for {} — skipping repository.", UserServiceInternal.class.getSimpleName(), uuid);
            return CompletableFuture.completedFuture(null);
        }

        final UserRecord preloaded = this.preloadCache.getIfPresent(uuid);
        if (preloaded != null) {
            this.logger.debug("[{}] preloadCache hit for {}.", UserServiceInternal.class.getSimpleName(), uuid);
            return CompletableFuture.completedFuture(null);
        }

        this.logger.debug("[{}] Cold miss for {} — querying repository.", UserServiceInternal.class.getSimpleName(), uuid);
        return this.repository.findById(uuid)
                .thenAccept(existing -> {
                    if (existing.isPresent()) {
                        this.preloadCache.put(uuid, existing.get());
                        this.logger.debug("[{}] Profile stored in preloadCache for {}.", UserServiceInternal.class.getSimpleName(), uuid);
                    } else {
                        this.logger.debug("[{}] No existing profile for {} (first join).", UserServiceInternal.class.getSimpleName(), uuid);
                    }
                })
                .whenComplete((_, exception) -> {
                    if (exception != null) {
                        this.logger.error("Failed to pre-load profile for UUID: {}; disconnecting.", uuid, exception);
                        disconnect.run();
                    }
                });
    }

    public CompletableFuture<Void> saveUser(final UUID uuid) {
        final var user = this.userCache.getIfPresent(uuid);
        if (user != null) {
            final var userRecord = new UserRecord(
                    user.uuid(),
                    user.name(),
                    user.lastSeen()
            );
            return this.repository.upsert(userRecord)
                    .whenComplete((_, exception) -> {
                        if (exception != null) {
                            this.logger.error("Failed save player record on disconnect for UUID: {}", uuid, exception);
                        }
                    });
        }

        return CompletableFuture.completedFuture(null);
    }

    @Override
    public Optional<PluginTemplateUser> getUser(final UUID uuid) {
        return Optional.ofNullable(this.userCache.getIfPresent(uuid));
    }

    @Override
    public <P extends Audience & Identified> CompletableFuture<PluginTemplateUser> loadUser(final P player) {
        final UUID uuid = player.get(Identity.UUID).orElseThrow(() -> new IllegalArgumentException());
        final String currentName = player.get(Identity.NAME).orElseThrow(() -> new IllegalArgumentException());

        final PluginTemplateUser cached = this.userCache.getIfPresent(uuid);
        if (cached != null) {
            this.logger.debug("[{}] Tier-1 (userCache) hit for {}.", UserServiceInternal.class.getSimpleName(), uuid);
            return CompletableFuture.completedFuture(cached);
        }

        final UserRecord preloaded = this.preloadCache.getIfPresent(uuid);
        if (preloaded != null) {
            this.logger.debug("[{}] Tier-2 (preloadCache) hit for {}.", UserServiceInternal.class.getSimpleName(), uuid);
            final PluginTemplateUser user = this.userFactory.createUser(player, preloaded);
            this.userCache.put(uuid, user);
            return CompletableFuture.completedFuture(user);
        }

        this.logger.debug("[{}] Tier-3 (repository) miss for {} — querying storage.", UserServiceInternal.class.getSimpleName(), uuid);
        return this.repository.findById(uuid)
                .thenApply(existing -> existing.orElseGet(() -> new UserRecord(uuid, currentName, Instant.now())))
                .thenApply(profile -> {
                    final PluginTemplateUser platformUser = this.userFactory.createUser(player, profile);
                    this.userCache.put(uuid, platformUser);
                    return platformUser;
                });
    }

    @Override
    public CompletableFuture<Void> deleteUser(final UUID uuid) {
        this.logger.debug("[{}] Removing all data for {}.", UserServiceInternal.class.getSimpleName(), uuid);
        this.userCache.invalidate(uuid);
        this.preloadCache.invalidate(uuid);
        return this.repository.delete(uuid);
    }

    private static final class OnlineAwareExpiry implements Expiry<UUID, PluginTemplateUser> {

        private static final long NEVER_EXPIRE_NANOS = Long.MAX_VALUE;

        private final long offlineExpireNanos;

        OnlineAwareExpiry(final PrimaryConfiguration.Storage.Cache cacheSettings) {
            this.offlineExpireNanos = cacheSettings.expireAfterOffline();
        }

        @Override
        public long expireAfterCreate(
                final UUID key, final PluginTemplateUser user, final long currentTime
        ) {
            return this.ttl(user);
        }

        @Override
        public long expireAfterUpdate(
                final UUID key, final PluginTemplateUser user,
                final long currentTime, final long currentDuration
        ) {
            return this.ttl(user);
        }

        @Override
        public long expireAfterRead(
                final UUID key, final PluginTemplateUser user,
                final long currentTime, final long currentDuration
        ) {
            return this.ttl(user);
        }

        private long ttl(final PluginTemplateUser user) {
            return user.isOnline() ? NEVER_EXPIRE_NANOS : this.offlineExpireNanos;
        }
    }
}

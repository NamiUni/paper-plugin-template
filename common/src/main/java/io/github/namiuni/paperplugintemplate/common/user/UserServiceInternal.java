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

import com.github.benmanes.caffeine.cache.AsyncCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import io.github.namiuni.paperplugintemplate.api.user.PluginTemplateUser;
import io.github.namiuni.paperplugintemplate.api.user.PluginTemplateUserService;
import io.github.namiuni.paperplugintemplate.common.Metadata;
import io.github.namiuni.paperplugintemplate.common.user.storage.UserRecord;
import io.github.namiuni.paperplugintemplate.common.user.storage.UserRepository;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.identity.Identity;
import org.jspecify.annotations.NullMarked;

@Singleton
@NullMarked
public final class UserServiceInternal implements PluginTemplateUserService {

    private final UserRepository repository;
    private final UserFactory userFactory;
    private final MojangAPI mojangAPI;

    private final Executor executor;
    private final AsyncCache<UUID, UserInternal> cache;

    @Inject
    UserServiceInternal(
            final UserRepository repository,
            final UserFactory userFactory,
            final Provider<UserConfiguration> config,
            final Metadata metadata,
            final MojangAPI mojangAPI
    ) {
        this.repository = repository;
        this.userFactory = userFactory;
        this.mojangAPI = mojangAPI;

        this.executor = Executors.newThreadPerTaskExecutor(
                Thread.ofVirtual().name(metadata.name() + "-User-Pool", 0).factory()
        );

        final var cacheSettings = config.get().cache();
        this.cache = Caffeine.newBuilder()
                .maximumSize(cacheSettings.maximumSize())
                .executor(this.executor)
                .expireAfter(new OnlineAwareExpiry(cacheSettings.expireAfterOffline()))
                .buildAsync();
    }

    public void invalidateUser(final UUID uuid) {
        this.cache.synchronous().invalidate(uuid);
    }

    @Override
    public Optional<CompletableFuture<PluginTemplateUser>> getCachedUser(final UUID uuid) {
        return Optional
                .ofNullable(this.cache.getIfPresent(uuid))
                .map(future -> future.thenApply(PluginTemplateUser.class::cast));
    }

    @Override
    public CompletableFuture<PluginTemplateUser> loadUserOrCreate(final Audience audience) {
        final UUID uuid = audience.get(Identity.UUID).orElseThrow();
        final CompletableFuture<UserInternal> cached = this.cache.getIfPresent(uuid);
        if (cached != null) {
            this.cache.put(uuid, cached
                    .thenApply(user -> user.withAudience(audience))
                    .whenComplete((user, exception) -> {
                        if (exception != null) {
                            this.repository.upsert(UserRecord.from(user));
                        }
                    })
            );
            return cached.thenApply(PluginTemplateUser.class::cast);
        }

        return this.cache
                .get(uuid, _ -> {
                    // TODO: impl Setting
                    final var currentRecord = this.repository.findById(uuid)
                             .orElseGet(() -> new UserRecord(
                                     uuid,
                                     this.mojangAPI.findName(uuid).orElse("Unknown"),
                                     Instant.ofEpochMilli(0))
                             );
                    final var user = this.userFactory.createUser(
                             audience,
                             currentRecord.uuid(),
                             currentRecord.name(),
                             currentRecord.lastSeen(),
                             new PluginTemplateUser.Setting() { }
                    );
                    final var updatedRecord = UserRecord.from(user);
                    if (!Objects.equals(currentRecord, updatedRecord)) {
                        this.repository.upsert(updatedRecord);
                    }

                    return user;
                })
                .thenApply(PluginTemplateUser.class::cast);
    }

    @Override
    public CompletableFuture<PluginTemplateUser> loadUserOrCreate(final UUID uuid) {
        return Optional
                .ofNullable(this.cache.getIfPresent(uuid))
                .orElseGet(() -> this.cache.get(uuid, _ -> {
                    // TODO: impl Setting
                    final var currentRecord = this.repository.findById(uuid)
                             .orElseGet(() -> new UserRecord(
                                     uuid,
                                     this.mojangAPI.findName(uuid).orElse("Unknown"),
                                     Instant.ofEpochMilli(0))
                             );
                    final var user = this.userFactory.createUser(
                            uuid,
                            currentRecord.name(),
                            currentRecord.lastSeen(),
                            new PluginTemplateUser.Setting() { }
                    );
                    final var updatedRecord = UserRecord.from(user);
                    if (!Objects.equals(currentRecord, updatedRecord)) {
                        this.repository.upsert(updatedRecord);
                    }

                    return user;
                }))
                .thenApply(PluginTemplateUser.class::cast);
    }

    @Override
    public CompletableFuture<Optional<PluginTemplateUser>> loadUserIfPresent(final UUID uuid) {
        final CompletableFuture<UserInternal> cached = this.cache.getIfPresent(uuid);
        if (cached != null) {
            return cached.thenApply(Optional::of);
        }

        final var stored = CompletableFuture.supplyAsync(() -> this.repository.findById(uuid), this.executor);

        return stored.thenCompose(existing -> existing
                .map(userRecord -> this.cache // TODO: impl Setting
                        .get(uuid, _ -> this.userFactory.createUser(
                                uuid,
                                userRecord.name(),
                                userRecord.lastSeen(),
                                new PluginTemplateUser.Setting() { })
                        )
                        .thenApply(PluginTemplateUser.class::cast)
                        .thenApply(Optional::of))
                .orElseGet(() -> CompletableFuture.completedFuture(Optional.empty())));
    }

    @Override
    public CompletableFuture<Void> saveUserIfPresent(final UUID uuid) {
        final var cached = this.cache.getIfPresent(uuid);
        if (cached != null) {
            return cached.thenAccept(user -> this.repository.upsert(UserRecord.from(user)));
        }

        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> deleteUserIfPresent(final UUID uuid) {
        this.invalidateUser(uuid);
        return CompletableFuture.runAsync(() -> this.repository.delete(uuid));
    }

    public Iterable<PluginTemplateUser> users() {
        return this.cache.synchronous().asMap().values().stream()
                .map(PluginTemplateUser.class::cast)
                .toList();
    }

    private static final class OnlineAwareExpiry implements Expiry<UUID, PluginTemplateUser> {

        private static final long NEVER_EXPIRE_NANOS = Long.MAX_VALUE;

        private final long offlineExpireNanos;

        OnlineAwareExpiry(final long offlineExpireNanos) {
            this.offlineExpireNanos = offlineExpireNanos;
        }

        @Override
        public long expireAfterCreate(final UUID key, final PluginTemplateUser user, final long currentTime) {
            return this.ttl(user);
        }

        @Override
        public long expireAfterUpdate(final UUID key, final PluginTemplateUser user, final long currentTime, final long currentDuration) {
            return this.ttl(user);
        }

        @Override
        public long expireAfterRead(final UUID key, final PluginTemplateUser user, final long currentTime, final long currentDuration) {
            return this.ttl(user);
        }

        private long ttl(final PluginTemplateUser user) {
            return user.isOnline() ? NEVER_EXPIRE_NANOS : this.offlineExpireNanos;
        }
    }
}

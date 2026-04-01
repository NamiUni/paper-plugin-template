/*
 * PaperPluginTemplate
 *
 * Copyright (c) 2026. Namiu (ãã«ããã)
 *                     Contributors
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
import io.github.namiuni.paperplugintemplate.api.user.PluginTemplateUser;
import io.github.namiuni.paperplugintemplate.api.user.PluginTemplateUserService;
import io.github.namiuni.paperplugintemplate.common.user.storage.UserProfile;
import io.github.namiuni.paperplugintemplate.common.user.storage.UserRepository;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.identity.Identified;
import net.kyori.adventure.identity.Identity;
import org.jspecify.annotations.NullMarked;

/// Application service for managing player [UserProfile] data.
///
/// Maintains an in-memory cache of online or recently active players to provide
/// strictly non-blocking reads. Cache misses and write operations are delegated
/// asynchronously to the injected [UserRepository].
///
/// Futures returned by write or load operations complete on the executor owned
/// by the active repository implementation.
///
/// @see UserRepository
@Singleton
@NullMarked
public final class PluginTemplateUserServiceInternal implements PluginTemplateUserService {

    private final UserRepository repository;
    private final Cache<UUID, UserProfile> cache;

    /// Constructs a new `UserService` and initializes a bounded in-memory cache.
    ///
    /// @param repository the active storage backend, selected at startup by
    ///                   [io.github.namiuni.paperplugintemplate.common.user.storage.StorageModule]
    @Inject
    private PluginTemplateUserServiceInternal(final UserRepository repository) {
        this.repository = repository;
        this.cache = Caffeine.newBuilder()
                .maximumSize(512L)
                .expireAfterAccess(15, TimeUnit.MINUTES)
                .build();
    }

    public CompletableFuture<Optional<UserProfile>> loadUserProfile(final UUID uuid) {
        final UserProfile cached = this.cache.getIfPresent(uuid);
        if (cached != null) {
            return CompletableFuture.completedFuture(Optional.of(cached));
        }

        final CompletableFuture<Optional<UserProfile>> future = this.repository.findById(uuid);
        future.thenAccept(existing -> existing.ifPresent(profile -> this.cache.put(uuid, profile)));
        return future;
    }

    public CompletableFuture<Void> upsertUser(final PluginTemplateUser user) {
        if (user instanceof PlatformUser<?> platformUser) {
            this.cache.put(user.uuid(), platformUser.profile());

            return this.repository.findById(user.uuid())
                    .thenCompose(existing -> existing
                            .map(_ -> this.repository.upsert(platformUser.profile()))
                            .orElse(CompletableFuture.completedFuture(null)));
        }
        return CompletableFuture.completedFuture(null);
    }

    public void discardUser(final UUID uuid) {
        this.cache.invalidate(uuid);
    }

    @Override
    public <P extends Audience & Identified> Optional<PluginTemplateUser> getUser(final P player) {
        return Optional.ofNullable(this.cache.getIfPresent(player.get(Identity.UUID).orElseThrow()))
                .map(cached -> new PlatformUser<>(player, cached));
    }

    @Override
    public <P extends Audience & Identified> CompletableFuture<PluginTemplateUser> loadUser(final P player) {
        // Cache
        final UUID uuid = player.get(Identity.UUID).orElseThrow();
        final UserProfile cached = this.cache.getIfPresent(uuid);
        if (cached != null) {
            return CompletableFuture.completedFuture(new PlatformUser<>(player, cached));
        }

        // Repository
        final var future = this.repository.findById(uuid)
                .thenApply(existing -> existing
                        .orElse(new UserProfile(uuid, player.get(Identity.NAME).orElseThrow(), Instant.now())));
        future.thenAccept(profile -> this.cache.put(uuid, profile));
        return future.thenApply(profile -> new PlatformUser<>(player, profile));
    }

    @Override
    public CompletableFuture<Void> deleteUser(final UUID uuid) {
        this.cache.invalidate(uuid);
        return this.repository.delete(uuid);
    }
}

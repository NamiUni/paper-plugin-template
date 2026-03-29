/*
 * paper-plugin-template
 *
 * Copyright (c) 2026. Namiu/Unitarou
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
package io.github.namiuni.paperplugintemplate.user;

import io.github.namiuni.paperplugintemplate.user.storage.UserProfile;
import io.github.namiuni.paperplugintemplate.user.storage.UserRepository;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/// TODO: Javadoc
@Singleton
@NullMarked
public final class UserService {

    private final UserRepository repository;

    /// TODO: Javadoc
    @Inject
    private UserService(final UserRepository repository) {
        this.repository = repository;
    }

    // -------------------------------------------------------------------------
    // Queries
    // -------------------------------------------------------------------------

    /// TODO: Javadoc
    public CompletableFuture<UserProfile> getOrCreateUser(final UUID uuid, final @Nullable String username) {
        return this.getUser(uuid)
                .thenApply(userProfile -> userProfile
                        .orElse(new UserProfile(uuid, Objects.requireNonNullElse(username, "Unknown"), Instant.now())));
    }

    /// TODO: Javadoc
    public CompletableFuture<Optional<UserProfile>> getUser(final UUID uuid) {
        return this.repository.findById(uuid);
    }

    // TODO: Javadoc
    public CompletableFuture<Void> upsertUser(final UserProfile userProfile) {
        return this.repository.upsert(userProfile);
    }

    // TODO: Javadoc
    public CompletableFuture<Void> deleteUser(final UserProfile userProfile) {
        return this.repository.delete(userProfile.uuid());
    }
}

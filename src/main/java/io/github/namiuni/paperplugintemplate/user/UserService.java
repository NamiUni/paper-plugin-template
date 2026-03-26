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

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.namiuni.paperplugintemplate.configuration.PrimaryConfiguration;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.UUID;
import java.util.function.Supplier;
import org.jspecify.annotations.NullMarked;

/// Application service responsible for managing the lifecycle of [TemplateUser] instances.
///
/// Uses a Caffeine [Cache] as an in-memory store keyed by player [UUID].
/// Entries are automatically evicted by Caffeine according to the configured eviction
/// policy, so callers should never assume a user is present in the cache without
/// checking first.
///
/// The primary configuration is injected as a [Supplier] rather than a direct
/// value so that configuration hot-reloads performed via
/// [io.github.namiuni.paperplugintemplate.commands.AdminCommand] are reflected
/// transparently without restarting this service.
@Singleton
@NullMarked
public final class UserService {

    private final Supplier<PrimaryConfiguration> primaryConfig;
    private final Cache<UUID, TemplateUser> cache;

    /// Constructs a new `UserService` and initialises the in-memory Caffeine cache.
    ///
    /// @param primaryConfig supplier of the current primary plugin configuration;
    ///                      evaluated lazily on each access so hot-reloads are respected
    @Inject
    private UserService(final Supplier<PrimaryConfiguration> primaryConfig) {
        this.primaryConfig = primaryConfig;
        this.cache = Caffeine.newBuilder().build();
    }
}

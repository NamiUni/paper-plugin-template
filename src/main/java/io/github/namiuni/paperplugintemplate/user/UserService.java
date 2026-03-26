/*
 * paper-plugin-template
 *
 * Copyright (c) 2025. Namiu/Unitarou
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
import io.github.namiuni.paperplugintemplate.configuration.ConfigurationHolder;
import io.github.namiuni.paperplugintemplate.configuration.PrimaryConfiguration;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.UUID;
import org.jspecify.annotations.NullMarked;

@Singleton
@NullMarked
public final class UserService {

    private final ConfigurationHolder<PrimaryConfiguration> primaryConfig;

    private final Cache<UUID, TemplateUser> cache;

    @Inject
    private UserService(final ConfigurationHolder<PrimaryConfiguration> primaryConfig) {
        this.primaryConfig = primaryConfig;
        this.cache = Caffeine.newBuilder().build();
    }
}

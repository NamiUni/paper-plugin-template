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
import io.github.namiuni.paperplugintemplate.api.user.PluginTemplateUser;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.NullMarked;

@Singleton
@NullMarked
public final class UserService {

    private final Cache<UUID, PluginTemplateUser> cache;

    @Inject
    UserService(final Cache<UUID, PluginTemplateUser> cache) {
        this.cache = cache;
    }

    public void putUser(final PluginTemplateUser user) {
        this.cache.put(user.uuid(), user);
    }

    public void removeUser(final UUID uuid) {
        this.cache.invalidate(uuid);
    }

    public Optional<PluginTemplateUser> getUser(final UUID uuid) {
        return Optional.ofNullable(this.cache.getIfPresent(uuid));
    }

    public Collection<PluginTemplateUser> getUsers() {
        return this.cache.asMap().values();
    }
}

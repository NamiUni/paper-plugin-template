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
import io.github.namiuni.paperplugintemplate.common.infrastructure.configuration.configurations.PrimaryConfiguration;
import io.github.namiuni.paperplugintemplate.common.infrastructure.storage.UserRecord;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.jspecify.annotations.NullMarked;

@Singleton
@NullMarked
public final class UserCache {

    private static final long NEVER_EXPIRE_NANOS = Long.MAX_VALUE;
    private static final long PRELOAD_EXPIRE_SECONDS = 30L;

    private final Cache<UUID, UserRecord> preloadCache;
    private final Cache<UUID, PluginTemplateUser> userCache;

    @Inject
    UserCache(final Provider<PrimaryConfiguration> primaryConfig) {
        final PrimaryConfiguration.Storage.Cache settings = primaryConfig.get().storage().userCache();

        this.preloadCache = Caffeine.newBuilder()
                .expireAfterWrite(PRELOAD_EXPIRE_SECONDS, TimeUnit.SECONDS)
                .build();

        this.userCache = Caffeine.newBuilder()
                .maximumSize(settings.maximumSize())
                .expireAfter(new OnlineAwareExpiry(settings.expireAfterOffline()))
                .build();
    }

    public Optional<PluginTemplateUser> getUser(final UUID uuid) {
        return Optional.ofNullable(this.userCache.getIfPresent(uuid));
    }

    public Optional<UserRecord> getPreloaded(final UUID uuid) {
        return Optional.ofNullable(this.preloadCache.getIfPresent(uuid));
    }

    public void cachePreloaded(final UUID uuid, final UserRecord record) {
        this.preloadCache.put(uuid, record);
    }

    public void cacheUser(final UUID uuid, final PluginTemplateUser user) {
        this.userCache.put(uuid, user);
    }

    public void invalidate(final UUID uuid) {
        this.userCache.invalidate(uuid);
        this.preloadCache.invalidate(uuid);
    }

    private static final class OnlineAwareExpiry implements Expiry<UUID, PluginTemplateUser> {

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

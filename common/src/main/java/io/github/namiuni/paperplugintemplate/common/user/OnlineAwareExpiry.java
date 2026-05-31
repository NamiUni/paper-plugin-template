package io.github.namiuni.paperplugintemplate.common.user;

import com.github.benmanes.caffeine.cache.Expiry;
import io.github.namiuni.paperplugintemplate.api.user.PluginTemplateUser;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import java.util.UUID;
import org.jspecify.annotations.NullMarked;

@NullMarked
final class OnlineAwareExpiry implements Expiry<UUID, PluginTemplateUser> {

    private static final long NEVER_EXPIRE_NANOS = Long.MAX_VALUE;

    private final Provider<UserConfig> userConfig;

    @Inject
    OnlineAwareExpiry(final Provider<UserConfig> userConfig) {
        this.userConfig = userConfig;
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
        return user.audience().isPresent() ? NEVER_EXPIRE_NANOS : this.userConfig.get().cache().expireAfterOffline().toNanos();
    }
}

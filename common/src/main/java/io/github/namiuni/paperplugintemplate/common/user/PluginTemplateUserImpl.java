package io.github.namiuni.paperplugintemplate.common.user;

import io.github.namiuni.paperplugintemplate.api.user.PluginTemplateUser;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.pointer.Pointers;
import net.kyori.adventure.text.Component;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class PluginTemplateUserImpl implements PluginTemplateUser {

    private final Pointers pointers;

    public PluginTemplateUserImpl(final Pointers pointers) {
        this.pointers = pointers;
    }

    @Override
    public Optional<Audience> audience() {
        return this.get(UserPointers.AUDIENCE);
    }

    @Override
    public UUID uuid() {
        return this.get(Identity.UUID).orElseThrow();
    }

    @Override
    public String name() {
        return this.getOrDefault(Identity.NAME, "Unknown");
    }

    @Override
    public Component displayName() {
        return this.getOrDefault(Identity.DISPLAY_NAME, Component.text(this.name()));
    }

    @Override
    public Locale locale() {
        return this.getOrDefault(Identity.LOCALE, Locale.US);
    }

    @Override
    public Instant lastSeen() {
        return this.getOrDefault(UserPointers.LAST_SEEN, Instant.ofEpochMilli(0));
    }

    @Override
    public Setting setting() {
        return this.getOrDefault(UserPointers.SETTING, UserSettingImpl.DEFAULT);
    }

    @Override
    public Identity identity() {
        return Identity.identity(this.uuid());
    }

    @Override
    public Pointers pointers() {
        return this.pointers;
    }

    @Override
    public boolean equals(final @Nullable Object other) {
        if (other instanceof final PluginTemplateUser that) {
            if (that == this) {
                return true;
            }

            return Objects.equals(that.uuid(), this.uuid());
        }

        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.pointers);
    }

    @Override
    public String toString() {
        return "UserInternal{" +
                "audience=" + this.audience() +
                ", uuid=" + this.uuid() +
                ", name=" + this.name() +
                ", displayName=" + this.displayName() +
                ", locale=" + this.locale() +
                ", lastSeen=" + this.lastSeen() +
                ", setting=" + this.setting() +
                ", identity=" + this.identity() +
                ", pointers=" + this.pointers() +
                '}';
    }
}

package io.github.namiuni.paperplugintemplate.common.user;

import io.github.namiuni.paperplugintemplate.api.user.PluginTemplateUser;
import java.time.Instant;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.pointer.Pointer;
import org.jspecify.annotations.NullMarked;

@NullMarked
public final class UserPointers {

    public static final Pointer<Audience> AUDIENCE = Pointer.pointer(
            Audience.class,
            Key.key("plugin_template", "audience")
    );
    public static final Pointer<Instant> LAST_SEEN = Pointer.pointer(
            Instant.class,
            Key.key("plugin_template", "last_seen")
    );
    public static final Pointer<PluginTemplateUser.Setting> SETTING = Pointer.pointer(
            PluginTemplateUser.Setting.class,
            Key.key("plugin_template", "setting")
    );

    private UserPointers() {
    }
}

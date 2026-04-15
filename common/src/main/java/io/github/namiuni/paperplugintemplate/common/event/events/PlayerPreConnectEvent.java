package io.github.namiuni.paperplugintemplate.common.event.events;

import java.util.UUID;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.jspecify.annotations.NullMarked;

@NullMarked
public record PlayerPreConnectEvent(
        UUID uuid,
        Audience audience,
        Disconnector disconnector
) implements Event {

    @FunctionalInterface
    public interface Disconnector {
        void disconnect(Component reason);
    }
}

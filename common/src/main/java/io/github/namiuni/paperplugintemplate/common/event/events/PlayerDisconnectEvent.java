package io.github.namiuni.paperplugintemplate.common.event.events;

import java.util.UUID;
import org.jspecify.annotations.NullMarked;

@NullMarked
public record PlayerDisconnectEvent(UUID uuid) implements Event {
}

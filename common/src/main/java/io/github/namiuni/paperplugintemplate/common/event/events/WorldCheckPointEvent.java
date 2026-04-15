package io.github.namiuni.paperplugintemplate.common.event.events;

import java.util.Set;
import java.util.UUID;
import org.jspecify.annotations.NullMarked;

@NullMarked
public record WorldCheckPointEvent(Set<UUID> onlinePlayerUuids) implements Event {
}

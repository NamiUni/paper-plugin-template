package io.github.namiuni.paperplugintemplate.common.event.events;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.identity.Identified;
import org.jspecify.annotations.NullMarked;

@NullMarked
public record PlayerConnectEvent<P extends Audience & Identified>(P player) implements Event {
}

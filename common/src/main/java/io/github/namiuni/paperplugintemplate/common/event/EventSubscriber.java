package io.github.namiuni.paperplugintemplate.common.event;

import io.github.namiuni.paperplugintemplate.common.event.events.Event;
import org.jspecify.annotations.NullMarked;

@NullMarked
@FunctionalInterface
public interface EventSubscriber<E extends Event> {

    void on(E event);
}

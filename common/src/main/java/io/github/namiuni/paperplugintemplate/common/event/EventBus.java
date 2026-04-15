package io.github.namiuni.paperplugintemplate.common.event;

import io.github.namiuni.paperplugintemplate.common.event.events.Event;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface EventBus {

    <E extends Event> void subscribe(Class<E> eventType, EventSubscriber<? super E> subscriber);

    <E extends Event> void unsubscribe(Class<E> eventType, EventSubscriber<? super E> subscriber);

    <E extends Event> void publish(E event);
}

package io.github.namiuni.paperplugintemplate.common.event;

import io.github.namiuni.paperplugintemplate.common.event.events.Event;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.jspecify.annotations.NullMarked;

@Singleton
@NullMarked
public final class SimpleEventBus implements EventBus {

    private final Map<Class<?>, List<EventSubscriber<?>>> subscribers = new ConcurrentHashMap<>();
    private final ComponentLogger logger;

    @Inject
    private SimpleEventBus(final ComponentLogger logger) {
        this.logger = logger;
    }

    @Override
    public <E extends Event> void subscribe(
            final Class<E> eventType,
            final EventSubscriber<? super E> subscriber
    ) {
        this.subscribers.computeIfAbsent(eventType, _ -> new CopyOnWriteArrayList<>()).add(subscriber);
    }

    @Override
    public <E extends Event> void unsubscribe(
            final Class<E> eventType,
            final EventSubscriber<? super E> subscriber
    ) {
        final List<EventSubscriber<?>> list = this.subscribers.get(eventType);
        if (list != null) {
            list.remove(subscriber);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <E extends Event> void publish(final E event) {
        final List<EventSubscriber<?>> list = this.subscribers.get(event.getClass());
        if (list == null || list.isEmpty()) {
            return;
        }
        for (final EventSubscriber<?> subscriber : list) {
            try {
                ((EventSubscriber<E>) subscriber).on(event);
            } catch (final Exception exception) {
                this.logger.error(
                        "[{}] Unhandled exception in subscriber for event {}",
                        SimpleEventBus.class.getSimpleName(),
                        event.getClass().getSimpleName(),
                        exception
                );
            }
        }
    }
}

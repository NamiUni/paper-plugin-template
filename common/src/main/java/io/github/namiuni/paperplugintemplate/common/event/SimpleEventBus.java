/*
 * PaperPluginTemplate
 *
 * Copyright (c) 2026. Namiu (うにたろう)
 *                     Contributors []
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
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
        java.util.Optional.ofNullable(this.subscribers.get(eventType))
                .ifPresent(list -> list.remove(subscriber));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <E extends Event> void publish(final E event) {
        final List<EventSubscriber<?>> list = this.subscribers.get(event.getClass());
        if (list == null || list.isEmpty()) {
            return;
        }

        list.forEach(subscriber -> {
            try {
                ((EventSubscriber<E>) subscriber).on(event);
            } catch (final Exception exception) {
                this.logger.error(
                        "Unhandled exception in subscriber for event {}",
                        event.getClass().getSimpleName(),
                        exception
                );
            }
        });
    }
}

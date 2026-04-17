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
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.jspecify.annotations.NullMarked;

@Singleton
@NullMarked
public final class SimpleEventBus implements EventBus {

    private final ConcurrentHashMap<Class<?>, EventSubscriber<?>[]> subscribers = new ConcurrentHashMap<>();

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
        this.subscribers.compute(eventType, (_, current) -> {
            if (current == null) {
                return new EventSubscriber<?>[]{subscriber};
            }

            for (final EventSubscriber<?> existing : current) {
                if (existing == subscriber) {
                    return current;
                }
            }

            final EventSubscriber<?>[] updated = Arrays.copyOf(current, current.length + 1);
            updated[current.length] = subscriber;
            return updated;
        });
    }

    @Override
    public <E extends Event> void unsubscribe(
            final Class<E> eventType,
            final EventSubscriber<? super E> subscriber
    ) {
        this.subscribers.computeIfPresent(eventType, (_, current) -> {
            int idx = -1;
            for (int i = 0; i < current.length; i++) {
                if (current[i] == subscriber) {
                    idx = i;
                    break;
                }
            }

            if (idx < 0) {
                return current;
            }

            if (current.length == 1) {
                return null;
            }

            final EventSubscriber<?>[] updated = new EventSubscriber<?>[current.length - 1];
            System.arraycopy(current, 0, updated, 0, idx);
            System.arraycopy(current, idx + 1, updated, idx, current.length - idx - 1);
            return updated;
        });
    }

    @Override
    public <E extends Event> void publish(final E event) {

        @SuppressWarnings("unchecked") final var snapshot =
                (EventSubscriber<? super E>[]) this.subscribers.get(event.getClass());

        if (snapshot == null) {
            return;
        }

        for (final EventSubscriber<? super E> subscriber : snapshot) {
            try {
                subscriber.on(event);
            } catch (final Exception exception) {
                this.logger.error(
                        "Unhandled exception in subscriber for event {}",
                        event.getClass().getSimpleName(),
                        exception
                );
            }
        }
    }
}

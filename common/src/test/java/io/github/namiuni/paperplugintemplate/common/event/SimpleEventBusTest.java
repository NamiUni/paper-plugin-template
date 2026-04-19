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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import io.github.namiuni.paperplugintemplate.common.event.events.Event;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SimpleEventBusTest {

    record TestEvent(String value) implements Event {
    }

    record OtherEvent(int number) implements Event {
    }

    private SimpleEventBus eventBus;

    @BeforeEach
    void setUp() {
        this.eventBus = new SimpleEventBus(mock(ComponentLogger.class));
    }

    @Test
    void subscribedHandlerReceivesPublishedEvent() {
        final List<String> received = new ArrayList<>();

        this.eventBus.subscribe(TestEvent.class, event -> received.add(event.value()));
        this.eventBus.publish(new TestEvent("hello"));

        assertEquals(List.of("hello"), received);
    }

    @Test
    void multipleSubscribersAllReceiveEvent() {
        final AtomicInteger counter = new AtomicInteger();

        this.eventBus.subscribe(TestEvent.class, _ -> counter.incrementAndGet());
        this.eventBus.subscribe(TestEvent.class, _ -> counter.incrementAndGet());
        this.eventBus.subscribe(TestEvent.class, _ -> counter.incrementAndGet());
        this.eventBus.publish(new TestEvent("x"));

        assertEquals(3, counter.get());
    }

    @Test
    void subscribersReceiveEventsInSubscriptionOrder() {
        final List<Integer> order = new ArrayList<>();

        this.eventBus.subscribe(TestEvent.class, _ -> order.add(1));
        this.eventBus.subscribe(TestEvent.class, _ -> order.add(2));
        this.eventBus.subscribe(TestEvent.class, _ -> order.add(3));
        this.eventBus.publish(new TestEvent("order"));

        assertEquals(List.of(1, 2, 3), order);
    }

    @Test
    void unsubscribedHandlerNoLongerReceivesEvents() {
        final AtomicInteger counter = new AtomicInteger();
        final EventSubscriber<TestEvent> subscriber = _ -> counter.incrementAndGet();

        this.eventBus.subscribe(TestEvent.class, subscriber);
        this.eventBus.publish(new TestEvent("first"));
        this.eventBus.unsubscribe(TestEvent.class, subscriber);
        this.eventBus.publish(new TestEvent("second"));

        assertEquals(1, counter.get());
    }

    @Test
    void unsubscribingNonRegisteredSubscriberIsNoOp() {
        final EventSubscriber<TestEvent> subscriber = _ -> {
        };

        // Should not throw
        this.eventBus.unsubscribe(TestEvent.class, subscriber);
    }

    @Test
    void sameSubscriberRegisteredOnlyOnce() {
        final AtomicInteger counter = new AtomicInteger();
        final EventSubscriber<TestEvent> subscriber = _ -> counter.incrementAndGet();

        this.eventBus.subscribe(TestEvent.class, subscriber);
        this.eventBus.subscribe(TestEvent.class, subscriber);
        this.eventBus.publish(new TestEvent("dedup"));

        assertEquals(1, counter.get());
    }

    @Test
    void publishingWithNoSubscribersIsNoOp() {
        // Should not throw
        this.eventBus.publish(new TestEvent("no-subscribers"));
    }

    @Test
    void exceptionInOneSubscriberDoesNotPreventOtherSubscribersFromReceiving() {
        final AtomicInteger successCounter = new AtomicInteger();

        this.eventBus.subscribe(TestEvent.class, _ -> {
            throw new RuntimeException("boom");
        });
        this.eventBus.subscribe(TestEvent.class, _ -> successCounter.incrementAndGet());
        this.eventBus.publish(new TestEvent("resilience"));

        assertEquals(1, successCounter.get());
    }

    @Test
    void subscribersForDifferentEventTypesAreIsolated() {
        final List<String> testEvents = new ArrayList<>();
        final List<Integer> otherEvents = new ArrayList<>();

        this.eventBus.subscribe(TestEvent.class, event -> testEvents.add(event.value()));
        this.eventBus.subscribe(OtherEvent.class, event -> otherEvents.add(event.number()));

        this.eventBus.publish(new TestEvent("only-test"));
        this.eventBus.publish(new OtherEvent(42));

        assertEquals(List.of("only-test"), testEvents);
        assertEquals(List.of(42), otherEvents);
    }

    @Test
    void lastRemainingSubscriberCanBeUnsubscribed() {
        final AtomicInteger counter = new AtomicInteger();
        final EventSubscriber<TestEvent> subscriber = _ -> counter.incrementAndGet();

        this.eventBus.subscribe(TestEvent.class, subscriber);
        this.eventBus.unsubscribe(TestEvent.class, subscriber);
        this.eventBus.publish(new TestEvent("after-last-unsub"));

        assertEquals(0, counter.get());
    }

    @Test
    void middleSubscriberCanBeRemovedFromMultiple() {
        final List<Integer> order = new ArrayList<>();

        final EventSubscriber<TestEvent> first = _ -> order.add(1);
        final EventSubscriber<TestEvent> middle = _ -> order.add(2);
        final EventSubscriber<TestEvent> last = _ -> order.add(3);

        this.eventBus.subscribe(TestEvent.class, first);
        this.eventBus.subscribe(TestEvent.class, middle);
        this.eventBus.subscribe(TestEvent.class, last);

        this.eventBus.unsubscribe(TestEvent.class, middle);
        this.eventBus.publish(new TestEvent("middle-removed"));

        assertEquals(List.of(1, 3), order);
    }

    @Test
    void exceptionInSubscriberIsLoggedWithEventClassName() {
        final ComponentLogger logger = mock(ComponentLogger.class);
        final SimpleEventBus bus = new SimpleEventBus(logger);
        final RuntimeException boom = new RuntimeException("boom");

        bus.subscribe(TestEvent.class, _ -> {
            throw boom;
        });
        bus.publish(new TestEvent("log-check"));

        verify(logger).error("Unhandled exception in subscriber for event {}", "TestEvent", boom);
    }
}

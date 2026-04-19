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
package io.github.namiuni.paperplugintemplate.common.user;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Scopes;
import com.google.inject.testing.fieldbinder.Bind;
import com.google.inject.testing.fieldbinder.BoundFieldModule;
import io.github.namiuni.paperplugintemplate.api.user.PluginTemplateUser;
import io.github.namiuni.paperplugintemplate.api.user.PluginTemplateUserService;
import io.github.namiuni.paperplugintemplate.common.TestPlayer;
import io.github.namiuni.paperplugintemplate.common.event.EventBus;
import io.github.namiuni.paperplugintemplate.common.event.SimpleEventBus;
import io.github.namiuni.paperplugintemplate.common.event.events.PlayerConnectEvent;
import io.github.namiuni.paperplugintemplate.common.event.events.PlayerDisconnectEvent;
import io.github.namiuni.paperplugintemplate.common.event.events.PlayerPreConnectEvent;
import io.github.namiuni.paperplugintemplate.common.event.events.WorldCheckPointEvent;
import io.github.namiuni.paperplugintemplate.common.infrastructure.translation.translations.MessageAssembly;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@NullMarked
@ExtendWith(MockitoExtension.class)
class UserSessionHandlerTest {

    private static final UUID UUID_A = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID UUID_B = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID UUID_C = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final TestPlayer PLAYER_A = new TestPlayer(UUID_A, "Alice");

    // @Bind fields are picked up by BoundFieldModule and registered as Guice bindings.
    // MockitoExtension initialises @Mock before @BeforeEach, so values are non-null
    // when BoundFieldModule.of(this) is evaluated.
    @Bind @Mock private UserPersistenceCoordinator persistenceCoordinator;
    @Bind @Mock private PluginTemplateUserService userService;
    @Bind @Mock private MessageAssembly messages;

    private SimpleEventBus eventBus;

    @BeforeEach
    void setUp() {
        // BoundFieldModule exposes the @Bind-annotated mocks as Guice bindings.
        // The inline module additionally binds EventBus -> SimpleEventBus so that
        // UserSessionHandler's constructor receives a real bus we can publish into.
        // Guice bypasses the package-private constructor of SimpleEventBus via
        // setAccessible reflection, which is the reason BoundFieldModule is used here.
        final Injector injector = Guice.createInjector(
                BoundFieldModule.of(this),
                new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(SimpleEventBus.class).in(Scopes.SINGLETON);
                        bind(EventBus.class).to(SimpleEventBus.class).in(Scopes.SINGLETON);
                        bind(ComponentLogger.class).toInstance(ComponentLogger.logger());
                    }
                }
        );

        this.eventBus = injector.getInstance(SimpleEventBus.class);

        // Instantiating UserSessionHandler wires all four event subscriptions.
        injector.getInstance(UserSessionHandler.class);
    }

    // ── PlayerPreConnectEvent ─────────────────────────────────────────────────

    @Test
    void preConnectEventTriggersPreloadWithCorrectUUID() {
        when(this.persistenceCoordinator.preload(any(), any()))
                .thenReturn(CompletableFuture.completedFuture(null));

        this.eventBus.publish(preConnectEvent(UUID_A));

        verify(this.persistenceCoordinator).preload(eq(UUID_A), any());
    }

    @Test
    void preConnectDisconnectCallbackInvokesDisconnectorWithProfileFailureMessage() {
        final Component failureMessage = Component.text("failure");
        final Audience audience = mock(Audience.class);
        final PlayerPreConnectEvent.Disconnector disconnector = mock(PlayerPreConnectEvent.Disconnector.class);

        when(this.messages.joinFailureProfile(audience)).thenReturn(failureMessage);

        // Capture the onFailure Runnable and execute it synchronously to simulate a
        // repository failure occurring inside preload.
        final ArgumentCaptor<Runnable> onFailureCaptor = ArgumentCaptor.forClass(Runnable.class);
        when(this.persistenceCoordinator.preload(any(), onFailureCaptor.capture()))
                .thenAnswer(_ -> {
                    onFailureCaptor.getValue().run();
                    return CompletableFuture.completedFuture(null);
                });

        this.eventBus.publish(new PlayerPreConnectEvent(UUID_A, audience, disconnector));

        verify(disconnector).disconnect(failureMessage);
    }

    // ── PlayerConnectEvent ────────────────────────────────────────────────────

    @Test
    void connectEventTriggersLoadUser() {
        when(this.userService.loadUser(PLAYER_A))
                .thenReturn(CompletableFuture.completedFuture(mock(PluginTemplateUser.class)));

        this.eventBus.publish(new PlayerConnectEvent<>(PLAYER_A));

        verify(this.userService).loadUser(PLAYER_A);
    }

    // ── PlayerDisconnectEvent ─────────────────────────────────────────────────

    @Test
    void disconnectEventTriggersSaveWithCorrectUUID() {
        when(this.persistenceCoordinator.save(UUID_A))
                .thenReturn(CompletableFuture.completedFuture(null));

        this.eventBus.publish(new PlayerDisconnectEvent(UUID_A));

        verify(this.persistenceCoordinator).save(UUID_A);
    }

    // ── WorldCheckPointEvent ──────────────────────────────────────────────────

    @Test
    void checkpointEventTriggersSaveForEachOnlinePlayer() {
        when(this.persistenceCoordinator.save(any()))
                .thenReturn(CompletableFuture.completedFuture(null));

        this.eventBus.publish(new WorldCheckPointEvent(Set.of(UUID_A, UUID_B, UUID_C)));

        verify(this.persistenceCoordinator).save(UUID_A);
        verify(this.persistenceCoordinator).save(UUID_B);
        verify(this.persistenceCoordinator).save(UUID_C);
    }

    @Test
    void checkpointEventWithEmptySetDoesNotCallSave() {
        this.eventBus.publish(new WorldCheckPointEvent(Set.of()));

        verify(this.persistenceCoordinator, never()).save(any());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static PlayerPreConnectEvent preConnectEvent(final UUID uuid) {
        return new PlayerPreConnectEvent(uuid, Audience.empty(), _ -> {});
    }
}

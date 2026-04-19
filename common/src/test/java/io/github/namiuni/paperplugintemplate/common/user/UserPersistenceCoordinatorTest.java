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

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import io.github.namiuni.paperplugintemplate.api.user.PluginTemplateUser;
import io.github.namiuni.paperplugintemplate.common.infrastructure.storage.UserRecord;
import io.github.namiuni.paperplugintemplate.common.infrastructure.storage.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserPersistenceCoordinatorTest {

    private static final UUID UUID_A = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final Instant FIXED_NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Mock
    private UserCache cache;
    @Mock
    private UserRepository repository;
    @Mock
    private ComponentLogger logger;

    private UserPersistenceCoordinator coordinator;

    @BeforeEach
    void setUp() {
        final Clock fixedClock = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);
        this.coordinator = new UserPersistenceCoordinator(this.cache, this.repository, fixedClock, this.logger);
    }

    // ── preload ───────────────────────────────────────────────────────────────

    @Test
    void preloadSkipsRepositoryWhenUserAlreadyInUserCache() {
        when(this.cache.getUser(UUID_A)).thenReturn(Optional.of(mock(PluginTemplateUser.class)));

        this.coordinator.preload(UUID_A, () -> {
        }).join();

        verify(this.repository, never()).findById(any());
    }

    @Test
    void preloadSkipsRepositoryWhenRecordAlreadyPreloaded() {
        when(this.cache.getUser(UUID_A)).thenReturn(Optional.empty());
        when(this.cache.getPreloaded(UUID_A)).thenReturn(Optional.of(
                new UserRecord(UUID_A, "Alice", Instant.EPOCH)
        ));

        this.coordinator.preload(UUID_A, () -> {
        }).join();

        verify(this.repository, never()).findById(any());
    }

    @Test
    void preloadQueriesRepositoryOnColdMiss() {
        when(this.cache.getUser(UUID_A)).thenReturn(Optional.empty());
        when(this.cache.getPreloaded(UUID_A)).thenReturn(Optional.empty());
        when(this.repository.findById(UUID_A)).thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        this.coordinator.preload(UUID_A, () -> {
        }).join();

        verify(this.repository).findById(UUID_A);
    }

    @Test
    void preloadStoresFoundRecordInPreloadCache() {
        final UserRecord record = new UserRecord(UUID_A, "Alice", Instant.EPOCH);
        when(this.cache.getUser(UUID_A)).thenReturn(Optional.empty());
        when(this.cache.getPreloaded(UUID_A)).thenReturn(Optional.empty());
        when(this.repository.findById(UUID_A)).thenReturn(CompletableFuture.completedFuture(Optional.of(record)));

        this.coordinator.preload(UUID_A, () -> {
        }).join();

        verify(this.cache).cachePreloaded(UUID_A, record);
    }

    @Test
    void preloadDoesNotCachePreloadedWhenRepositoryReturnsEmpty() {
        when(this.cache.getUser(UUID_A)).thenReturn(Optional.empty());
        when(this.cache.getPreloaded(UUID_A)).thenReturn(Optional.empty());
        when(this.repository.findById(UUID_A)).thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        this.coordinator.preload(UUID_A, () -> {
        }).join();

        verify(this.cache, never()).cachePreloaded(any(), any());
    }

    @Test
    void preloadInvokesOnFailureCallbackWhenRepositoryFails() {
        final AtomicBoolean called = new AtomicBoolean(false);
        final CompletableFuture<Optional<UserRecord>> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("db-down"));
        when(this.repository.findById(UUID_A)).thenReturn(failedFuture);
        assertThrows(Exception.class, () -> this.coordinator.preload(UUID_A, () -> called.set(true)).join());

        assertTrue(called.get());
    }

    @Test
    void preloadDoesNotInvokeOnFailureWhenRepositorySucceeds() {
        when(this.cache.getUser(UUID_A)).thenReturn(Optional.empty());
        when(this.cache.getPreloaded(UUID_A)).thenReturn(Optional.empty());
        when(this.repository.findById(UUID_A)).thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        final AtomicBoolean disconnected = new AtomicBoolean(false);
        this.coordinator.preload(UUID_A, () -> disconnected.set(true)).join();

        assertFalse(disconnected.get());
    }

    // ── save ─────────────────────────────────────────────────────────────────

    @Test
    void saveDoesNothingWhenUserNotInCache() {
        when(this.cache.getUser(UUID_A)).thenReturn(Optional.empty());

        this.coordinator.save(UUID_A).join();

        verify(this.repository, never()).upsert(any());
    }

    @Test
    void saveUpsertsRecordWhenUserIsInCache() {
        final PluginTemplateUser user = mock(PluginTemplateUser.class);
        when(user.uuid()).thenReturn(UUID_A);
        when(user.name()).thenReturn("Alice");
        when(this.cache.getUser(UUID_A)).thenReturn(Optional.of(user));
        when(this.repository.upsert(any())).thenReturn(CompletableFuture.completedFuture(null));

        this.coordinator.save(UUID_A).join();

        verify(this.repository, times(1)).upsert(any());
    }

    @Test
    void saveUsesFixedClockForLastSeen() {
        final PluginTemplateUser user = mock(PluginTemplateUser.class);
        when(user.uuid()).thenReturn(UUID_A);
        when(user.name()).thenReturn("Alice");
        when(this.cache.getUser(UUID_A)).thenReturn(Optional.of(user));
        when(this.repository.upsert(any())).thenReturn(CompletableFuture.completedFuture(null));

        this.coordinator.save(UUID_A).join();

        final ArgumentCaptor<UserRecord> captor = ArgumentCaptor.forClass(UserRecord.class);
        verify(this.repository).upsert(captor.capture());
        assertEquals(FIXED_NOW, captor.getValue().lastSeen());
    }

    @Test
    void savePreservesUUIDAndNameFromCachedUser() {
        final PluginTemplateUser user = mock(PluginTemplateUser.class);
        when(user.uuid()).thenReturn(UUID_A);
        when(user.name()).thenReturn("Alice");
        when(this.cache.getUser(UUID_A)).thenReturn(Optional.of(user));
        when(this.repository.upsert(any())).thenReturn(CompletableFuture.completedFuture(null));

        this.coordinator.save(UUID_A).join();

        final ArgumentCaptor<UserRecord> captor = ArgumentCaptor.forClass(UserRecord.class);
        verify(this.repository).upsert(captor.capture());
        assertEquals(UUID_A, captor.getValue().uuid());
        assertEquals("Alice", captor.getValue().name());
    }
}

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.namiuni.paperplugintemplate.api.user.PluginTemplateUser;
import io.github.namiuni.paperplugintemplate.common.TestPlayer;
import io.github.namiuni.paperplugintemplate.common.infrastructure.storage.UserRecord;
import io.github.namiuni.paperplugintemplate.common.infrastructure.storage.UserRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserServiceInternalTest {

    private static final UUID UUID_A = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final TestPlayer PLAYER_A = new TestPlayer(UUID_A, "Alice");

    @Mock
    private UserCache cache;
    @Mock
    private UserRepository repository;
    @Mock
    private UserFactory userFactory;
    @Mock
    private ComponentLogger logger;

    private UserServiceInternal service;

    @BeforeEach
    void setUp() {
        this.service = new UserServiceInternal(this.cache, this.repository, this.userFactory, this.logger);
    }

    // ── getUser ───────────────────────────────────────────────────────────────

    @Test
    void getUserDelegatesToCache() {
        final PluginTemplateUser user = mock(PluginTemplateUser.class);
        when(this.cache.getUser(UUID_A)).thenReturn(Optional.of(user));

        assertEquals(user, this.service.getUser(UUID_A).orElseThrow());
    }

    @Test
    void getUserReturnsEmptyWhenCacheMiss() {
        when(this.cache.getUser(UUID_A)).thenReturn(Optional.empty());

        assertTrue(this.service.getUser(UUID_A).isEmpty());
    }

    // ── loadUser: tier-1 userCache hit ────────────────────────────────────────

    @Test
    void loadUserReturnsCachedUserWithoutHittingRepository() {
        final PluginTemplateUser cached = mock(PluginTemplateUser.class);
        when(this.cache.getUser(UUID_A)).thenReturn(Optional.of(cached));

        final PluginTemplateUser result = this.service.loadUser(PLAYER_A).join();

        assertEquals(cached, result);
        verify(this.repository, never()).findById(any());
        verify(this.userFactory, never()).createUser(any(), any());
    }

    // ── loadUser: tier-2 preloadCache hit ─────────────────────────────────────

    @Test
    void loadUserUsesPreloadedRecordWithoutHittingRepository() {
        final UserRecord preloaded = new UserRecord(UUID_A, "Alice", Instant.EPOCH);
        final PluginTemplateUser createdUser = mock(PluginTemplateUser.class);

        when(this.cache.getUser(UUID_A)).thenReturn(Optional.empty());
        when(this.cache.getPreloaded(UUID_A)).thenReturn(Optional.of(preloaded));
        when(this.userFactory.createUser(same(PLAYER_A), eq(preloaded))).thenReturn(createdUser);

        final PluginTemplateUser result = this.service.loadUser(PLAYER_A).join();

        assertEquals(createdUser, result);
        verify(this.repository, never()).findById(any());
    }

    @Test
    void loadUserCachesUserFromPreloadTier() {
        final UserRecord preloaded = new UserRecord(UUID_A, "Alice", Instant.EPOCH);
        final PluginTemplateUser createdUser = mock(PluginTemplateUser.class);

        when(this.cache.getUser(UUID_A)).thenReturn(Optional.empty());
        when(this.cache.getPreloaded(UUID_A)).thenReturn(Optional.of(preloaded));
        when(this.userFactory.createUser(any(), any())).thenReturn(createdUser);

        this.service.loadUser(PLAYER_A).join();

        verify(this.cache).cacheUser(UUID_A, createdUser);
    }

    // ── loadUser: tier-3 repository miss ──────────────────────────────────────

    @Test
    void loadUserQueriesRepositoryOnTotalCacheMiss() {
        final UserRecord dbRecord = new UserRecord(UUID_A, "Alice", Instant.EPOCH);
        final PluginTemplateUser createdUser = mock(PluginTemplateUser.class);

        when(this.cache.getUser(UUID_A)).thenReturn(Optional.empty());
        when(this.cache.getPreloaded(UUID_A)).thenReturn(Optional.empty());
        when(this.repository.findById(UUID_A)).thenReturn(CompletableFuture.completedFuture(Optional.of(dbRecord)));
        when(this.userFactory.createUser(same(PLAYER_A), eq(dbRecord))).thenReturn(createdUser);

        final PluginTemplateUser result = this.service.loadUser(PLAYER_A).join();

        assertEquals(createdUser, result);
        verify(this.repository).findById(UUID_A);
    }

    @Test
    void loadUserCreatesNewRecordWhenNotInRepository() {
        when(this.cache.getUser(UUID_A)).thenReturn(Optional.empty());
        when(this.cache.getPreloaded(UUID_A)).thenReturn(Optional.empty());
        when(this.repository.findById(UUID_A)).thenReturn(CompletableFuture.completedFuture(Optional.empty()));
        when(this.userFactory.createUser(any(), any())).thenReturn(mock(PluginTemplateUser.class));

        this.service.loadUser(PLAYER_A).join();

        // Factory must be called with a brand-new record using the player's current identity
        verify(this.userFactory).createUser(same(PLAYER_A), any(UserRecord.class));
    }

    @Test
    void loadUserNewRecordUsesPlayerUUIDAndName() {
        when(this.cache.getUser(UUID_A)).thenReturn(Optional.empty());
        when(this.cache.getPreloaded(UUID_A)).thenReturn(Optional.empty());
        when(this.repository.findById(UUID_A)).thenReturn(CompletableFuture.completedFuture(Optional.empty()));
        when(this.userFactory.createUser(any(), any())).thenReturn(mock(PluginTemplateUser.class));

        this.service.loadUser(PLAYER_A).join();

        verify(this.userFactory).createUser(
                same(PLAYER_A),
                argThat(record -> UUID_A.equals(record.uuid()) && "Alice".equals(record.name()))
        );
    }

    @Test
    void loadUserCachesUserFromRepositoryTier() {
        final PluginTemplateUser createdUser = mock(PluginTemplateUser.class);

        when(this.cache.getUser(UUID_A)).thenReturn(Optional.empty());
        when(this.cache.getPreloaded(UUID_A)).thenReturn(Optional.empty());
        when(this.repository.findById(UUID_A)).thenReturn(CompletableFuture.completedFuture(Optional.empty()));
        when(this.userFactory.createUser(any(), any())).thenReturn(createdUser);

        this.service.loadUser(PLAYER_A).join();

        verify(this.cache).cacheUser(UUID_A, createdUser);
    }

    // ── deleteUser ────────────────────────────────────────────────────────────

    @Test
    void deleteUserInvalidatesCacheBeforeRepositoryDelete() {
        when(this.repository.delete(UUID_A)).thenReturn(CompletableFuture.completedFuture(null));

        this.service.deleteUser(UUID_A).join();

        verify(this.cache).invalidate(UUID_A);
        verify(this.repository).delete(UUID_A);
    }

    @Test
    void deleteUserAlwaysInvalidatesCacheEvenIfRepositoryFails() {
        final CompletableFuture<Void> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("db-down"));
        when(this.repository.delete(UUID_A)).thenReturn(failedFuture);

        try {
            this.service.deleteUser(UUID_A).join();
        } catch (final Exception _) {
            // expected — repository threw
        }

        verify(this.cache).invalidate(UUID_A);
    }

    // ── helper — Mockito argThat shorthand ────────────────────────────────────

    private static UserRecord argThat(final java.util.function.Predicate<UserRecord> predicate) {
        return org.mockito.ArgumentMatchers.argThat(predicate::test);
    }
}

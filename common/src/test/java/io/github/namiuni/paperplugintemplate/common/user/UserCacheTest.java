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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.namiuni.paperplugintemplate.api.user.PluginTemplateUser;
import io.github.namiuni.paperplugintemplate.common.infrastructure.configuration.configurations.PrimaryConfiguration;
import io.github.namiuni.paperplugintemplate.common.infrastructure.storage.StorageType;
import io.github.namiuni.paperplugintemplate.common.infrastructure.storage.UserRecord;
import jakarta.inject.Provider;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UserCacheTest {

    private static final UUID UUID_A = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID UUID_B = UUID.fromString("00000000-0000-0000-0000-000000000002");

    private UserCache cache;

    @BeforeEach
    void setUp() {
        final Provider<PrimaryConfiguration> configProvider = () -> buildConfig(100L, TimeUnit.MINUTES.toNanos(15L));
        this.cache = new UserCache(configProvider);
    }

    // ── getUser ──────────────────────────────────────────────────────────────

    @Test
    void getUserReturnsEmptyWhenNothingCached() {
        assertTrue(this.cache.getUser(UUID_A).isEmpty());
    }

    @Test
    void getUserReturnsCachedUser() {
        final PluginTemplateUser user = onlineUser(UUID_A, "Alice");

        this.cache.cacheUser(UUID_A, user);

        assertEquals(user, this.cache.getUser(UUID_A).orElseThrow());
    }

    @Test
    void getUserDoesNotReturnUserFromDifferentUUID() {
        final PluginTemplateUser user = onlineUser(UUID_A, "Alice");

        this.cache.cacheUser(UUID_A, user);

        assertTrue(this.cache.getUser(UUID_B).isEmpty());
    }

    // ── getPreloaded ─────────────────────────────────────────────────────────

    @Test
    void getPreloadedReturnsEmptyWhenNothingCached() {
        assertTrue(this.cache.getPreloaded(UUID_A).isEmpty());
    }

    @Test
    void getPreloadedReturnsCachedRecord() {
        final UserRecord record = new UserRecord(UUID_A, "Alice", Instant.EPOCH);

        this.cache.cachePreloaded(UUID_A, record);

        assertEquals(record, this.cache.getPreloaded(UUID_A).orElseThrow());
    }

    @Test
    void getPreloadedDoesNotReturnRecordFromDifferentUUID() {
        this.cache.cachePreloaded(UUID_A, new UserRecord(UUID_A, "Alice", Instant.EPOCH));

        assertTrue(this.cache.getPreloaded(UUID_B).isEmpty());
    }

    // ── invalidate ────────────────────────────────────────────────────────────

    @Test
    void invalidateRemovesCachedUser() {
        this.cache.cacheUser(UUID_A, onlineUser(UUID_A, "Alice"));
        this.cache.invalidate(UUID_A);

        assertTrue(this.cache.getUser(UUID_A).isEmpty());
    }

    @Test
    void invalidateRemovesCachedPreload() {
        this.cache.cachePreloaded(UUID_A, new UserRecord(UUID_A, "Alice", Instant.EPOCH));
        this.cache.invalidate(UUID_A);

        assertTrue(this.cache.getPreloaded(UUID_A).isEmpty());
    }

    @Test
    void invalidateOnlyAffectsTargetUUID() {
        final PluginTemplateUser alice = onlineUser(UUID_A, "Alice");
        final PluginTemplateUser bob = onlineUser(UUID_B, "Bob");

        this.cache.cacheUser(UUID_A, alice);
        this.cache.cacheUser(UUID_B, bob);
        this.cache.invalidate(UUID_A);

        assertTrue(this.cache.getUser(UUID_A).isEmpty());
        assertEquals(bob, this.cache.getUser(UUID_B).orElseThrow());
    }

    @Test
    void invalidateOnUncachedUUIDIsNoOp() {
        // Should not throw
        this.cache.invalidate(UUID_A);
    }

    @Test
    void userCacheAndPreloadCacheAreIndependent() {
        final PluginTemplateUser user = onlineUser(UUID_A, "Alice");
        final UserRecord record = new UserRecord(UUID_A, "Alice", Instant.EPOCH);

        this.cache.cacheUser(UUID_A, user);
        this.cache.cachePreloaded(UUID_A, record);

        assertEquals(user, this.cache.getUser(UUID_A).orElseThrow());
        assertEquals(record, this.cache.getPreloaded(UUID_A).orElseThrow());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static PluginTemplateUser onlineUser(final UUID uuid, final String name) {
        final PluginTemplateUser user = mock(PluginTemplateUser.class);
        when(user.uuid()).thenReturn(uuid);
        when(user.name()).thenReturn(name);
        when(user.isOnline()).thenReturn(true);
        return user;
    }

    private static PrimaryConfiguration buildConfig(final long maxSize, final long expireAfterOfflineNanos) {
        final var cacheSettings = new PrimaryConfiguration.Storage.Cache(maxSize, expireAfterOfflineNanos);
        final var poolSettings = new PrimaryConfiguration.Storage.Pool(8, 8, 1_800_000L, 0L, 1_800_000L);
        final var storage = new PrimaryConfiguration.Storage(
                StorageType.H2, "localhost", 3306, "test", "root", "", poolSettings, cacheSettings
        );
        return new PrimaryConfiguration(storage);
    }
}

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
import io.github.namiuni.paperplugintemplate.common.infrastructure.configuration.configurations.CommandConfiguration;
import io.github.namiuni.paperplugintemplate.common.infrastructure.configuration.configurations.PrimaryConfiguration;
import io.github.namiuni.paperplugintemplate.common.infrastructure.configuration.configurations.StorageConfiguration;
import io.github.namiuni.paperplugintemplate.common.infrastructure.storage.StorageType;
import io.github.namiuni.paperplugintemplate.common.user.storage.UserRecord;
import jakarta.inject.Provider;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import net.kyori.adventure.text.Component;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@NullMarked
class UserCacheTest {

    private static final UUID UUID_A = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID UUID_B = UUID.fromString("00000000-0000-0000-0000-000000000002");

    private UserCache cache;

    @BeforeEach
    void setUp() {
        final Provider<PrimaryConfiguration> configProvider = () -> buildConfig(100L, TimeUnit.MINUTES.toNanos(15L), 30L);
        this.cache = new UserCache(configProvider);
    }

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
        this.cache.cacheUser(UUID_A, onlineUser(UUID_A, "Alice"));

        assertTrue(this.cache.getUser(UUID_B).isEmpty());
    }

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

    private static PluginTemplateUser onlineUser(final UUID uuid, final String name) {
        final PluginTemplateUser user = mock(PluginTemplateUser.class);
        when(user.uuid()).thenReturn(uuid);
        when(user.name()).thenReturn(name);
        when(user.isOnline()).thenReturn(true);
        return user;
    }

    private static PrimaryConfiguration buildConfig(
            final long maxSize,
            final long expireAfterOfflineNanos,
            final long preloadExpireSeconds
    ) {
        final var cacheSettings = new StorageConfiguration.Cache(maxSize, expireAfterOfflineNanos, preloadExpireSeconds);
        final var poolSettings = new StorageConfiguration.Pool(8, 8, 1_800_000L, 0L, 1_800_000L);
        final var storage = new StorageConfiguration(
                StorageType.H2, "localhost", 3306, "test", "root", "", poolSettings, cacheSettings
        );

        final var command = new CommandConfiguration(
                new CommandConfiguration.Admin(
                        Component.translatable("commands.template.description", ""), // TODO
                        List.of("template", "papertemplate", "plugintemplate"),
                        new CommandConfiguration.Admin.Reload(
                                Component.translatable("commands.template.reload.description", "Reloads plugin configuration."),
                                List.of(),
                                Map.ofEntries(
                                        Map.entry("success", Component.translatable("commands.template.reload.success", "Configuration reloaded successfully.")),
                                        Map.entry("failure", Component.translatable("commands.template.reload.failure", "Failed to reload configuration. See the console for details."))
                                )
                        ),
                        new CommandConfiguration.Admin.Help(
                                Component.translatable("commands.template.help.description", "Displays help for plugin commands."),
                                List.of(),
                                Map.ofEntries(
                                        Map.entry("arguments", Component.translatable("commands.template.help.arguments", "Arguments")),
                                        Map.entry("available_commands", Component.translatable("commands.template.help.available_commands", "Available Commands")),
                                        Map.entry("click_for_next_page", Component.translatable("commands.template.help.click_for_next_page", "Click for next page")),
                                        Map.entry("click_for_previous_page", Component.translatable("commands.template.help.click_for_previous_page", "Click for previous page")),
                                        Map.entry("click_to_show_help", Component.translatable("commands.template.help.click_to_show_help", "Click to show help for this command")),
                                        Map.entry("command", Component.translatable("commands.template.help.command", "Command")),
                                        Map.entry("description", Component.translatable("commands.template.help.description", "Description")),
                                        Map.entry("help", Component.translatable("commands.template.help.help", "Help")),
                                        Map.entry("no_description", Component.translatable("commands.template.help.no_description", "No Description")),
                                        Map.entry("no_results_for_query", Component.translatable("commands.template.help.no_results_for_query", "No results for query")),
                                        Map.entry("optional", Component.translatable("commands.template.help.optional", "Optional")),
                                        Map.entry("page_out_of_range", Component.translatable("commands.template.help.page_out_of_range", "Error: Page <page> is not in range. Must be in range [1, <max_pages>]")),
                                        Map.entry("showing_results_for_query", Component.translatable("commands.template.help.showing_results_for_query", "Showing search results for query"))
                                ),
                                new CommandConfiguration.Admin.Help.Colors(
                                        "#2D7D9A",
                                        "#49E1E8",
                                        "#E3008C",
                                        "#FFFFFF",
                                        "#7D7D7D"
                                )
                        )
                )
        );
        return new PrimaryConfiguration(storage, command);
    }
}

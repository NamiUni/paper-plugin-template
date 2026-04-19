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
package io.github.namiuni.paperplugintemplate.common.infrastructure.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import io.github.namiuni.paperplugintemplate.common.infrastructure.configuration.configurations.PrimaryConfiguration;
import io.github.namiuni.paperplugintemplate.common.infrastructure.storage.StorageType;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@NullMarked
class ConfigurationHolderTest {

    @TempDir
    Path tempDir;

    private ConfigurationHolder<PrimaryConfiguration> holder;

    @BeforeEach
    void setUp() {
        final ConfigurationLoader<PrimaryConfiguration> loader = new ConfigurationLoader<>(
                PrimaryConfiguration.class,
                PrimaryConfiguration.DEFAULT,
                this.tempDir,
                MiniMessage.miniMessage(),
                mock(ComponentLogger.class)
        );
        this.holder = new ConfigurationHolder<>(loader, mock(ComponentLogger.class));
    }

    // ── get: initial state ────────────────────────────────────────────────────

    @Test
    void getReturnsNonNullAfterConstruction() {
        assertNotNull(this.holder.get());
    }

    @Test
    void getReturnsDefaultStorageType() {
        assertEquals(StorageType.H2, this.holder.get().storage().type());
    }

    @Test
    void getIsIdempotent() {
        assertSame(this.holder.get(), this.holder.get());
    }

    @Test
    void getReturnsDefaultHost() {
        assertEquals(
                PrimaryConfiguration.DEFAULT.storage().host(),
                this.holder.get().storage().host()
        );
    }

    // ── reload: happy path ────────────────────────────────────────────────────

    @Test
    void reloadReturnsNonNull() {
        assertNotNull(this.holder.reload());
    }

    @Test
    void reloadReturnsEquivalentStorageType() {
        assertEquals(StorageType.H2, this.holder.reload().storage().type());
    }

    @Test
    void reloadUpdatesGetResult() {
        this.holder.reload();
        assertNotNull(this.holder.get());
    }

    @Test
    void getAfterReloadPreservesStorageType() {
        this.holder.reload();
        assertEquals(StorageType.H2, this.holder.get().storage().type());
    }

    @Test
    void multipleReloadsAreStable() {
        for (int i = 0; i < 5; i++) {
            this.holder.reload();
        }
        assertEquals(StorageType.H2, this.holder.get().storage().type());
    }

    // ── reload: error propagation ─────────────────────────────────────────────

    @Test
    void reloadThrowsUncheckedConfigurateExceptionWhenFileIsCorrupted() throws Exception {
        // Allow the holder to load and write the config file first
        this.holder.get();
        // Overwrite with a type-breaking value
        Files.writeString(this.tempDir.resolve("config.conf"), """
                storage {
                    port = "not-a-number"
                }
                """);

        assertThrows(UncheckedConfigurateException.class, this.holder::reload);
    }

    @Test
    void getStillReturnsLastGoodConfigAfterFailedReload() throws Exception {
        final PrimaryConfiguration good = this.holder.get();
        Files.writeString(this.tempDir.resolve("config.conf"), "storage { port = \"bad\" }");

        try {
            this.holder.reload();
        } catch (final UncheckedConfigurateException ignored) {
            // expected
        }

        // AtomicReference must not have been updated to null / partial state
        assertSame(good, this.holder.get());
    }

    // ── thread safety ─────────────────────────────────────────────────────────

    @Test
    void concurrentGetsReturnConsistentValue() throws Exception {
        final int threads = 20;
        final CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(threads)) {
            final List<Future<PrimaryConfiguration>> futures = new ArrayList<>(threads);

            for (int i = 0; i < threads; i++) {
                futures.add(executor.submit(() -> {
                    start.await();
                    return this.holder.get();
                }));
            }
            start.countDown();

            final PrimaryConfiguration first = futures.getFirst().get();
            for (final Future<PrimaryConfiguration> future : futures) {
                assertSame(first, future.get());
            }
            executor.shutdown();
        }
    }
}

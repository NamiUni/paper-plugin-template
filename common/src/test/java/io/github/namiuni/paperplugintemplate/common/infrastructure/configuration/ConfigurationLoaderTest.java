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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import io.github.namiuni.paperplugintemplate.common.infrastructure.configuration.configurations.PrimaryConfiguration;
import io.github.namiuni.paperplugintemplate.common.infrastructure.storage.StorageType;
import java.nio.file.Files;
import java.nio.file.Path;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@NullMarked
class ConfigurationLoaderTest {

    @TempDir
    Path tempDir;

    private ConfigurationLoader<PrimaryConfiguration> loader;

    @BeforeEach
    void setUp() {
        this.loader = new ConfigurationLoader<>(
                PrimaryConfiguration.class,
                PrimaryConfiguration.DEFAULT,
                this.tempDir,
                MiniMessage.miniMessage(),
                mock(ComponentLogger.class)
        );
    }

    // ── configName ────────────────────────────────────────────────────────────

    @Test
    void configNameReturnsValueFromAnnotation() {
        assertEquals("config.conf", this.loader.configName());
    }

    // ── loadConfiguration: initial load (no file on disk) ────────────────────

    @Test
    void loadConfigurationReturnsNonNullWhenFileAbsent() {
        assertNotNull(this.loader.loadConfiguration());
    }

    @Test
    void loadConfigurationReturnsDefaultStorageTypeWhenFileAbsent() {
        assertEquals(StorageType.H2, this.loader.loadConfiguration().storage().type());
    }

    @Test
    void loadConfigurationReturnsDefaultHostWhenFileAbsent() {
        assertEquals(
                PrimaryConfiguration.DEFAULT.storage().host(),
                this.loader.loadConfiguration().storage().host()
        );
    }

    @Test
    void loadConfigurationReturnsDefaultPortWhenFileAbsent() {
        assertEquals(
                PrimaryConfiguration.DEFAULT.storage().port(),
                this.loader.loadConfiguration().storage().port()
        );
    }

    @Test
    void loadConfigurationReturnsDefaultDatabaseNameWhenFileAbsent() {
        assertEquals(
                PrimaryConfiguration.DEFAULT.storage().database(),
                this.loader.loadConfiguration().storage().database()
        );
    }

    @Test
    void loadConfigurationReturnsDefaultPoolMaxSizeWhenFileAbsent() {
        assertEquals(
                PrimaryConfiguration.DEFAULT.storage().pool().maximumPoolSize(),
                this.loader.loadConfiguration().storage().pool().maximumPoolSize()
        );
    }

    @Test
    void loadConfigurationReturnsDefaultCacheMaxSizeWhenFileAbsent() {
        assertEquals(
                PrimaryConfiguration.DEFAULT.storage().userCache().maximumSize(),
                this.loader.loadConfiguration().storage().userCache().maximumSize()
        );
    }

    // ── loadConfiguration: file creation side-effect ─────────────────────────

    @Test
    void loadConfigurationCreatesFileOnDisk() {
        this.loader.loadConfiguration();

        assertTrue(Files.exists(this.tempDir.resolve("config.conf")));
    }

    @Test
    void loadConfigurationWritesNonEmptyFile() throws Exception {
        this.loader.loadConfiguration();

        assertTrue(Files.size(this.tempDir.resolve("config.conf")) > 0);
    }

    // ── loadConfiguration: round-trip ────────────────────────────────────────

    @Test
    void loadConfigurationRoundtripsStorageType() {
        this.loader.loadConfiguration();
        assertEquals(StorageType.H2, this.loader.loadConfiguration().storage().type());
    }

    @Test
    void loadConfigurationRoundtripsHost() {
        this.loader.loadConfiguration();
        assertEquals(
                PrimaryConfiguration.DEFAULT.storage().host(),
                this.loader.loadConfiguration().storage().host()
        );
    }

    @Test
    void loadConfigurationRoundtripsPort() {
        this.loader.loadConfiguration();
        assertEquals(
                PrimaryConfiguration.DEFAULT.storage().port(),
                this.loader.loadConfiguration().storage().port()
        );
    }

    @Test
    void loadConfigurationRoundtripsPoolMinimumIdle() {
        this.loader.loadConfiguration();
        assertEquals(
                PrimaryConfiguration.DEFAULT.storage().pool().minimumIdle(),
                this.loader.loadConfiguration().storage().pool().minimumIdle()
        );
    }

    @Test
    void loadConfigurationRoundtripsPoolConnectionTimeout() {
        this.loader.loadConfiguration();
        assertEquals(
                PrimaryConfiguration.DEFAULT.storage().pool().connectionTimeout(),
                this.loader.loadConfiguration().storage().pool().connectionTimeout()
        );
    }

    @Test
    void loadConfigurationRoundtripsCacheExpireAfterOffline() {
        this.loader.loadConfiguration();
        assertEquals(
                PrimaryConfiguration.DEFAULT.storage().userCache().expireAfterOffline(),
                this.loader.loadConfiguration().storage().userCache().expireAfterOffline()
        );
    }

    // ── loadConfiguration: error handling ────────────────────────────────────

    @Test
    void loadConfigurationThrowsWhenFileContainsUnparsableContent() throws Exception {
        // Write content that breaks HOCON's type mapping for a known numeric field
        Files.writeString(this.tempDir.resolve("config.conf"), """
                storage {
                    port = "not-a-number"
                }
                """);

        assertThrows(UncheckedConfigurateException.class, this.loader::loadConfiguration);
    }
}

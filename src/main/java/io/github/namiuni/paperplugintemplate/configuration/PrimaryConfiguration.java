/*
 * paper-plugin-template
 *
 * Copyright (c) 2026. Namiu/Unitarou
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
package io.github.namiuni.paperplugintemplate.configuration;

import io.github.namiuni.paperplugintemplate.configuration.annotations.ConfigHeader;
import io.github.namiuni.paperplugintemplate.configuration.annotations.ConfigName;
import io.github.namiuni.paperplugintemplate.user.storage.StorageType;
import org.jspecify.annotations.NullMarked;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

/// Root configuration record for the template plugin.
///
/// This record is automatically serialized to and deserialized from
/// `config.conf` in the plugin data directory by
/// [ConfigurationLoader]. Add configuration fields as record components
/// and Configurate will handle the file mapping.
///
/// Use [#DEFAULT] as the fallback value when the file does not yet exist
/// or when a field is missing from an existing file.
@NullMarked
@ConfigSerializable
@ConfigName("config.conf")
@ConfigHeader("""
        Main configuration
        
        Restart the server after changing storage settings.
        """)
public record PrimaryConfiguration(
        @Comment("Storage backend configuration.")
        StorageConfig storage
) {

    /// Default instance used as a fallback when no configuration file is present.
    public static final PrimaryConfiguration DEFAULT = new PrimaryConfiguration(StorageConfig.DEFAULT);

    /// Configuration for the storage backend.
    ///
    /// Supports H2 (embedded, no external server required), MySQL (external server),
    /// and JSON (flat files, human-readable). H2 is recommended for single-server
    /// deployments; MySQL for networks sharing a database.
    @ConfigSerializable
    public record StorageConfig(
            @Comment("""
                    Storage type. Available options: H2, MYSQL, JSON
                    H2   - Embedded SQL database. No external server required.
                    MYSQL - External MySQL/MariaDB server.
                    JSON  - Flat JSON files. Human-readable, not suitable for high load.
                    """)
            StorageType type,

            @Comment("Database host. Only used for MYSQL.")
            String host,

            @Comment("Database port. Only used for MYSQL.")
            int port,

            @Comment("Database name. Used for H2 (file name) and MYSQL.")
            String database,

            @Comment("Database username. Only used for MYSQL.")
            String username,

            @Comment("Database password. Only used for MYSQL.")
            String password,

            @Comment("Maximum number of connections in the pool. Only used for MYSQL and H2.")
            int maximumPoolSize
    ) {

        /// Default storage configuration using H2.
        public static final StorageConfig DEFAULT = new StorageConfig(
                StorageType.H2,
                "localhost",
                3306,
                "your_plugin",
                "root",
                "",
                10
        );
    }
}

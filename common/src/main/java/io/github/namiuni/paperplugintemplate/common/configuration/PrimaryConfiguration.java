/*
 * PaperPluginTemplate
 *
 * Copyright (c) 2026. Namiu (うにたろう)
 *                     Contributors
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
package io.github.namiuni.paperplugintemplate.common.configuration;

import io.github.namiuni.paperplugintemplate.common.configuration.annotations.ConfigHeader;
import io.github.namiuni.paperplugintemplate.common.configuration.annotations.ConfigName;
import io.github.namiuni.paperplugintemplate.common.user.storage.StorageType;
import java.util.concurrent.TimeUnit;
import org.jspecify.annotations.NullMarked;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

/// Root configuration record for the template plugin.
///
/// This record is automatically serialized to and deserialized from
/// `config.conf` in the plugin data directory by [ConfigurationLoader].
/// Add configuration fields as record components and Configurate will handle
/// the file mapping.
///
/// Use [#DEFAULT] as the fallback value when the file does not yet exist
/// or when a field is missing from an existing file.
///
/// @param storage the storage backend configuration
@NullMarked
@ConfigSerializable
@ConfigName("config.conf")
@ConfigHeader("""
        Main configuration
        
        Restart the server after changing storage settings.
        """)
public record PrimaryConfiguration(
        @Comment("Storage backend configuration.")
        Storage storage
) {

    /// Default instance used as a fallback when no configuration file is
    /// present.
    public static final PrimaryConfiguration DEFAULT = new PrimaryConfiguration(
            new Storage(
                    StorageType.H2,
                    "localhost",
                    3306,
                    "paper_plugin_template", // TODO: change the database name
                    "server",
                    "",
                    new Storage.Pool(
                            8,
                            8,
                            TimeUnit.MINUTES.toMillis(30L),
                            TimeUnit.MINUTES.toMillis(0L),
                            TimeUnit.MINUTES.toMillis(30L)
                    ),
                    new Storage.Cache(
                            100L,
                            TimeUnit.MINUTES.toNanos(15L)
                    )
            )
    );

    /// Configuration for the storage backend.
    ///
    /// Supports H2 (embedded, no external server required), MySQL (external
    /// server), PostgreSQL (external server), and JSON (flat files,
    /// human-readable). H2 is recommended for single-server deployments;
    /// MySQL or PostgreSQL for networks sharing a database.
    ///
    /// @param type            the storage backend type
    /// @param host            the database host; used only for `MYSQL` and `POSTGRESQL`
    /// @param port            the database port; used only for `MYSQL` and `POSTGRESQL`
    /// @param database        the database name; used for `H2` (file name), `MYSQL`, and `POSTGRESQL`
    /// @param username        the database username; used only for `MYSQL` and `POSTGRESQL`
    /// @param password        the database password; used only for `MYSQL` and `POSTGRESQL`
    /// @param pool            TODO
    /// @param userCache           TODO
    @ConfigSerializable
    public record Storage(
            @Comment("""
                    Storage type. Available options: H2, MYSQL, POSTGRESQL, JSON
                    H2         - Embedded SQL database. No external server required.
                    MYSQL      - External MySQL/MariaDB server.
                    POSTGRESQL - External PostgreSQL server.
                    JSON       - Flat JSON files. Human-readable, not suitable for high load.
                    """)
            StorageType type,

            @Comment("Database host. Only used for MYSQL and POSTGRESQL.")
            String host,

            @Comment("Database port. Only used for MYSQL and POSTGRESQL.")
            int port,

            @Comment("Database name. Used for H2 (file name), MYSQL, and POSTGRESQL.")
            String database,

            @Comment("Database username. Only used for MYSQL and POSTGRESQL.")
            String username,

            @Comment("Database password. Only used for MYSQL and POSTGRESQL.")
            String password,

            @Comment("") //TODO
            Pool pool,

            @Comment("") //TODO
            Cache userCache
    ) {

        // TODO: Javadoc
        @ConfigSerializable
        public record Pool(

                @Comment("") // TODO
                int maximumPoolSize,

                @Comment("") // TODO
                int minimumIdle,

                @Comment("") // TODO
                long maximumLifetime,

                @Comment("") // TODO
                long keepaliveTime,

                @Comment("") // TODO
                long connectionTimeout
        ) {
        }

        // TODO: Javadoc
        @ConfigSerializable
        public record Cache(

                @Comment("") // TODO
                long maximumSize,

                @Comment("") // TODO
                long expireAfterOffline
        ) {
        }
    }
}

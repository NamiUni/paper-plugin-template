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
package io.github.namiuni.paperplugintemplate.common.infrastructure.configuration.configurations;

import io.github.namiuni.paperplugintemplate.common.infrastructure.configuration.annotations.ConfigHeader;
import io.github.namiuni.paperplugintemplate.common.infrastructure.configuration.annotations.ConfigName;
import io.github.namiuni.paperplugintemplate.common.infrastructure.persistence.StorageType;
import java.util.concurrent.TimeUnit;
import org.jspecify.annotations.NullMarked;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

/// Root configuration record for the template plugin.
///
/// This record is automatically serialized to and deserialized from
/// `config.conf` in the plugin data directory by [io.github.namiuni.paperplugintemplate.common.infrastructure.configuration.ConfigurationLoader].
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
    /// @param type      the storage backend type
    /// @param host      the database host; used only for `MYSQL` and `POSTGRESQL`
    /// @param port      the database port; used only for `MYSQL` and `POSTGRESQL`
    /// @param database  the database name; used for `H2` (file name), `MYSQL`, and `POSTGRESQL`
    /// @param username  the database username; used only for `MYSQL` and `POSTGRESQL`
    /// @param password  the database password; used only for `MYSQL` and `POSTGRESQL`
    /// @param pool      HikariCP connection-pool tuning parameters
    /// @param userCache in-memory user-profile cache sizing and eviction parameters
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

            @Comment("HikariCP connection pool settings.")
            Pool pool,

            @Comment("In-memory user-profile cache settings.")
            Cache userCache
    ) {

        /// HikariCP connection-pool tuning parameters.
        ///
        /// Controls the lifecycle and sizing of the JDBC connection pool
        /// managed by [com.zaxxer.hikari.HikariDataSource]. All time values
        /// are expressed in milliseconds and correspond directly to the
        /// HikariCP properties of the same names.
        ///
        /// Refer to the
        /// [HikariCP documentation](https://github.com/brettwooldridge/HikariCP#gear-configuration-knobs-baby)
        /// for an authoritative description of each property's effect on
        /// connection management and pool health.
        ///
        /// @param maximumPoolSize  the maximum number of connections the pool
        ///                         will maintain; set equal to `minimumIdle`
        ///                         for a fixed-size pool
        /// @param minimumIdle      the minimum number of idle connections the
        ///                         pool attempts to maintain; HikariCP
        ///                         recommends setting this equal to
        ///                         `maximumPoolSize` to avoid pool-sizing
        ///                         overhead
        /// @param maximumLifetime  the maximum lifetime of a connection in the
        ///                         pool in milliseconds; connections older
        ///                         than this value are retired and replaced;
        ///                         must be shorter than any database or
        ///                         infrastructure-imposed `wait_timeout`
        /// @param keepaliveTime    the interval in milliseconds at which a
        ///                         keepalive query is sent on idle connections
        ///                         to prevent them from being closed by the
        ///                         database or a network proxy; `0` disables
        ///                         keepalive
        /// @param connectionTimeout the maximum number of milliseconds a
        ///                          caller waits for a connection from the
        ///                          pool before a [java.sql.SQLException] is
        ///                          thrown
        @ConfigSerializable
        public record Pool(

                @Comment("""
                        Maximum number of connections the pool will maintain.
                        Set equal to minimumIdle for a fixed-size pool.
                        """)
                int maximumPoolSize,

                @Comment("""
                        Minimum number of idle connections maintained in the pool.
                        HikariCP recommends setting this equal to maximumPoolSize.
                        """)
                int minimumIdle,

                @Comment("""
                        Maximum lifetime of a connection in the pool (milliseconds).
                        Must be shorter than the database's wait_timeout value.
                        """)
                long maximumLifetime,

                @Comment("""
                        Interval between keepalive queries on idle connections (milliseconds).
                        Set to 0 to disable keepalive.
                        """)
                long keepaliveTime,

                @Comment("Maximum milliseconds a caller waits for a connection before an exception is thrown.")
                long connectionTimeout
        ) {
        }

        /// In-memory Caffeine cache sizing and eviction parameters for the
        /// user-profile cache.
        ///
        /// The user-profile cache holds [io.github.namiuni.paperplugintemplate.api.user.PluginTemplateUser]
        /// instances for the duration of a player's session and for a
        /// configurable window after they disconnect. Two eviction rules
        /// govern entry lifetimes:
        ///
        /// - **Online players** are pinned indefinitely; they are evicted
        ///   only via explicit invalidation on disconnect.
        /// - **Offline entries** are evicted `expireAfterOffline` nanoseconds
        ///   after their last cache interaction, bounding memory growth for
        ///   servers with frequent transient lookups (e.g. admin commands
        ///   targeting offline players).
        ///
        /// @param maximumSize         the maximum number of entries the cache
        ///                            may hold; when this limit is reached,
        ///                            Caffeine evicts entries according to a
        ///                            size-based least-recently-used policy
        /// @param expireAfterOffline  the duration in nanoseconds after which
        ///                            an offline player's cache entry expires
        ///                            following their last access; must be
        ///                            positive
        @ConfigSerializable
        public record Cache(

                @Comment("Maximum number of user entries held in the in-memory cache.")
                long maximumSize,

                @Comment("""
                        Duration in nanoseconds before an offline player's cache entry expires
                        after their last access. Does not affect online players.
                        """)
                long expireAfterOffline
        ) {
        }
    }
}

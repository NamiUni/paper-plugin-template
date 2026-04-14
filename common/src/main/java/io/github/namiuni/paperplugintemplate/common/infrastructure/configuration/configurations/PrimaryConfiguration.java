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

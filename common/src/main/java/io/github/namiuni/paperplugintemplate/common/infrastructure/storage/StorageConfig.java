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
package io.github.namiuni.paperplugintemplate.common.infrastructure.storage;

import io.github.namiuni.paperplugintemplate.common.infrastructure.configuration.annotations.ConfigHeader;
import io.github.namiuni.paperplugintemplate.common.infrastructure.configuration.annotations.ConfigName;
import java.time.Duration;
import org.jspecify.annotations.NullMarked;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

@NullMarked
@ConfigSerializable
@ConfigName("storage.conf")
@ConfigHeader("")
public record StorageConfig(
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
        Pool pool
) {

    public static final StorageConfig DEFAULT = new StorageConfig(
            StorageType.H2,
            "localhost",
            3306,
            "paper_plugin_template", // TODO: change the database name
            "server",
            "",
            new StorageConfig.Pool(
                    8,
                    8,
                    Duration.ofMinutes(30),
                    Duration.ZERO,
                    Duration.ofSeconds(30)
            )
    );

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
                    Maximum lifetime of a connection in the pool.
                    Must be shorter than the database's wait_timeout value.
                    Examples: 30m, 1h, 1h30m
                    """)
            Duration maximumLifetime,

            @Comment("""
                    Interval between keepalive queries on idle connections.
                    Set to 0s to disable keepalive.
                    Examples: 0s, 1m, 5m
                    """)
            Duration keepaliveTime,

            @Comment("""
                    Maximum time a caller waits for a connection before an exception is thrown.
                    Examples: 30s, 1m
                    """)
            Duration connectionTimeout
    ) {
    }
}

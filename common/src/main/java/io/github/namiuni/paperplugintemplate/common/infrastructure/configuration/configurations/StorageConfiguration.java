package io.github.namiuni.paperplugintemplate.common.infrastructure.configuration.configurations;

import io.github.namiuni.paperplugintemplate.common.infrastructure.storage.StorageType;
import org.jspecify.annotations.NullMarked;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

@NullMarked
@ConfigSerializable
public record StorageConfiguration(
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
}

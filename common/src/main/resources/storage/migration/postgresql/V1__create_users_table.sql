-- V1__create_users_table.sql  (PostgreSQL)
--
-- uuid uses PostgreSQL's native UUID type.  The PostgreSQL JDBC driver
-- serialises java.util.UUID objects to and from the wire format transparently,
-- requiring no byte-order encoding on the application side.  Native UUID
-- columns also benefit from efficient B-tree / hash indexing without a
-- wrapper function.
--
-- last_seen is stored as an epoch-millisecond BIGINT for consistency with the
-- MySQL dialect and to avoid TIMESTAMPTZ ↔ Instant conversion complexity.

CREATE TABLE IF NOT EXISTS users
(
    uuid      UUID        NOT NULL,
    name      VARCHAR(16) NOT NULL,
    last_seen BIGINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (uuid)
);

-- V1__create_users_table.sql  (MySQL / H2 in MODE=MySQL)
--
-- uuid is stored as BINARY(16): the raw 128-bit UUID bytes produced by the
-- MySql dialect argument factory (most-significant 8 bytes || least-significant
-- 8 bytes, big-endian).  A fixed-width binary column is more compact than
-- VARCHAR(36) and avoids charset / collation overhead.
--
-- last_seen is stored as an epoch-millisecond BIGINT for cross-database
-- compatibility between H2 and MySQL/MariaDB.

CREATE TABLE IF NOT EXISTS `users` (
    `uuid`      BINARY(16)  NOT NULL,
    `name`      VARCHAR(16) NOT NULL,
    `last_seen` BIGINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`uuid`)
);

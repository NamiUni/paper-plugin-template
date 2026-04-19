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
package io.github.namiuni.paperplugintemplate.common.infrastructure.storage.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.namiuni.paperplugintemplate.common.infrastructure.storage.StorageDialect;
import io.github.namiuni.paperplugintemplate.common.infrastructure.storage.UserRecord;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.jdbi.v3.cache.caffeine.CaffeineCachePlugin;
import org.jdbi.v3.core.argument.Arguments;
import org.jdbi.v3.core.argument.QualifiedArgumentFactory;
import org.jdbi.v3.core.mapper.RowMappers;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.jdbi.v3.testing.junit5.JdbiExtension;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@NullMarked
class UserDaoTest {

    private static final StorageDialect.MySQL DIALECT = new StorageDialect.MySQL();

    // Instant argument factory — mirrors the lambda in StorageModule exactly.
    private static final QualifiedArgumentFactory INSTANT_ARGUMENT = (_, value, _) -> {
        if (!(value instanceof final Instant instant)) {
            return Optional.empty();
        }
        return Optional.of((pos, stmt, _) -> stmt.setLong(pos, instant.toEpochMilli()));
    };

    // JdbiExtension manages the H2 in-memory database lifecycle per test method.
    // withConfig calls mirror StorageModule#jdbi; withInitializer runs the DDL
    // that Flyway would normally apply (V1__create_users_table.sql, MySQL variant).
    @SuppressWarnings("JUnitMalformedDeclaration")
    @RegisterExtension
    final JdbiExtension jdbiExtension = JdbiExtension.h2()
        .withPlugin(new SqlObjectPlugin())
        .withPlugin(new CaffeineCachePlugin())
        .withConfig(RowMappers.class, rm -> rm.register(UserRecord.class, DIALECT.profileMapper()))
        .withConfig(Arguments.class, args -> args.register(DIALECT.uuidArgumentFactory()))
        .withConfig(Arguments.class, args -> args.register(INSTANT_ARGUMENT))
        .withInitializer((_, handle) -> handle.execute("""
            CREATE TABLE IF NOT EXISTS users (
                uuid      BINARY(16)  NOT NULL,
                name      VARCHAR(16) NOT NULL,
                last_seen BIGINT      NOT NULL DEFAULT 0,
                PRIMARY KEY (uuid)
            )
        """));

    private static final UUID UUID_A = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID UUID_B = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final Instant T0 = Instant.ofEpochMilli(1_000L);
    private static final Instant T1 = Instant.ofEpochMilli(2_000L);

    private UserDao dao;

    @BeforeEach
    void setUp() {
        this.dao = this.jdbiExtension.getJdbi().onDemand(UserDao.class);
    }

    // ── findByUuid ────────────────────────────────────────────────────────────

    @Test
    void findByUuidReturnsEmptyWhenTableIsEmpty() {
        assertTrue(this.dao.findByUuid(UUID_A).isEmpty());
    }

    @Test
    void findByUuidReturnsInsertedRecord() {
        final UserRecord record = new UserRecord(UUID_A, "Alice", T0);
        this.dao.insert(record);

        final UserRecord found = this.dao.findByUuid(UUID_A).orElseThrow();

        assertEquals(UUID_A, found.uuid());
        assertEquals("Alice", found.name());
        assertEquals(T0, found.lastSeen());
    }

    @Test
    void findByUuidDoesNotReturnRecordForDifferentUUID() {
        this.dao.insert(new UserRecord(UUID_A, "Alice", T0));

        assertTrue(this.dao.findByUuid(UUID_B).isEmpty());
    }

    // ── insert ────────────────────────────────────────────────────────────────

    @Test
    void insertPersistsAllFields() {
        final UserRecord record = new UserRecord(UUID_B, "Bob", T1);
        this.dao.insert(record);

        final UserRecord found = this.dao.findByUuid(UUID_B).orElseThrow();

        assertEquals(UUID_B, found.uuid());
        assertEquals("Bob", found.name());
        assertEquals(T1, found.lastSeen());
    }

    // ── update ────────────────────────────────────────────────────────────────

    @Test
    void updateModifiesExistingRecord() {
        this.dao.insert(new UserRecord(UUID_A, "Alice", T0));

        this.dao.update(new UserRecord(UUID_A, "Alice-v2", T1));

        final UserRecord found = this.dao.findByUuid(UUID_A).orElseThrow();
        assertEquals("Alice-v2", found.name());
        assertEquals(T1, found.lastSeen());
    }

    @Test
    void updateReturnsOneForExistingRecord() {
        this.dao.insert(new UserRecord(UUID_A, "Alice", T0));

        final int affected = this.dao.update(new UserRecord(UUID_A, "Alice-v2", T1));

        assertEquals(1, affected);
    }

    @Test
    void updateReturnsZeroForNonExistentRecord() {
        final int affected = this.dao.update(new UserRecord(UUID_A, "Ghost", T0));

        assertEquals(0, affected);
    }

    // ── deleteByUuid ──────────────────────────────────────────────────────────

    @Test
    void deleteByUuidRemovesExistingRecord() {
        this.dao.insert(new UserRecord(UUID_A, "Alice", T0));
        this.dao.deleteByUuid(UUID_A);

        assertTrue(this.dao.findByUuid(UUID_A).isEmpty());
    }

    @Test
    void deleteByUuidOnNonExistentRecordIsNoOp() {
        // Must not throw
        this.dao.deleteByUuid(UUID_A);
    }

    @Test
    void deleteByUuidOnlyRemovesTargetRow() {
        this.dao.insert(new UserRecord(UUID_A, "Alice", T0));
        this.dao.insert(new UserRecord(UUID_B, "Bob", T0));

        this.dao.deleteByUuid(UUID_A);

        assertTrue(this.dao.findByUuid(UUID_A).isEmpty());
        assertTrue(this.dao.findByUuid(UUID_B).isPresent());
    }

    // ── upsert (default method) ───────────────────────────────────────────────

    @Test
    void upsertInsertsNewRecord() {
        this.dao.upsert(new UserRecord(UUID_A, "Alice", T0));

        assertTrue(this.dao.findByUuid(UUID_A).isPresent());
    }

    @Test
    void upsertUpdatesExistingRecord() {
        this.dao.upsert(new UserRecord(UUID_A, "Alice", T0));
        this.dao.upsert(new UserRecord(UUID_A, "Alice-v2", T1));

        final UserRecord found = this.dao.findByUuid(UUID_A).orElseThrow();
        assertEquals("Alice-v2", found.name());
        assertEquals(T1, found.lastSeen());
    }

    @Test
    void upsertIsIdempotentOnSameRecord() {
        final UserRecord record = new UserRecord(UUID_A, "Alice", T0);
        this.dao.upsert(record);
        this.dao.upsert(record);

        assertEquals(record, this.dao.findByUuid(UUID_A).orElseThrow());
    }
}

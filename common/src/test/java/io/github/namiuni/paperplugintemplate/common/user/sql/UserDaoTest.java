package io.github.namiuni.paperplugintemplate.common.user.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.namiuni.paperplugintemplate.common.infrastructure.storage.StorageDialect;
import io.github.namiuni.paperplugintemplate.common.user.UserRecord;
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

    private static final QualifiedArgumentFactory INSTANT_ARGUMENT = (_, value, _) -> {
        if (!(value instanceof final Instant instant)) {
            return Optional.empty();
        }
        return Optional.of((pos, stmt, _) -> stmt.setLong(pos, instant.toEpochMilli()));
    };

    @SuppressWarnings("JUnitMalformedDeclaration")
    @RegisterExtension
    final JdbiExtension jdbiExtension = JdbiExtension.h2()
            .withPlugin(new SqlObjectPlugin())
            .withPlugin(new CaffeineCachePlugin())
            .withConfig(RowMappers.class, rm -> rm.register(UserRecord.class, UserDao.rowMapper(DIALECT)))
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

    @Test
    void insertPersistsAllFields() {
        final UserRecord record = new UserRecord(UUID_B, "Bob", T1);
        this.dao.insert(record);

        final UserRecord found = this.dao.findByUuid(UUID_B).orElseThrow();

        assertEquals(UUID_B, found.uuid());
        assertEquals("Bob", found.name());
        assertEquals(T1, found.lastSeen());
    }

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

        assertEquals(1, this.dao.update(new UserRecord(UUID_A, "Alice-v2", T1)));
    }

    @Test
    void updateReturnsZeroForNonExistentRecord() {
        assertEquals(0, this.dao.update(new UserRecord(UUID_A, "Ghost", T0)));
    }

    @Test
    void deleteByUuidRemovesExistingRecord() {
        this.dao.insert(new UserRecord(UUID_A, "Alice", T0));
        this.dao.deleteByUuid(UUID_A);

        assertTrue(this.dao.findByUuid(UUID_A).isEmpty());
    }

    @Test
    void deleteByUuidOnNonExistentRecordIsNoOp() {
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

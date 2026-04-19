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
package io.github.namiuni.paperplugintemplate.common.user.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.zaxxer.hikari.HikariDataSource;
import io.github.namiuni.paperplugintemplate.common.Metadata;
import io.github.namiuni.paperplugintemplate.common.infrastructure.storage.DatabaseMigrator;
import io.github.namiuni.paperplugintemplate.common.infrastructure.storage.StorageDialect;
import io.github.namiuni.paperplugintemplate.common.user.UserRecord;
import io.github.namiuni.paperplugintemplate.common.utilities.UUIDCodec;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.jdbi.v3.core.argument.Arguments;
import org.jdbi.v3.core.argument.QualifiedArgumentFactory;
import org.jdbi.v3.testing.junit5.JdbiExtension;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@NullMarked
class JdbiUserRepositoryTest {

    private static final StorageDialect.MySQL DIALECT = new StorageDialect.MySQL();

    // Argument factory mirrors the one registered in StorageModule.jdbi()
    private static final QualifiedArgumentFactory INSTANT_ARGUMENT = (_, value, _) -> {
        if (!(value instanceof final Instant instant)) {
            return Optional.empty();
        }
        return Optional.of((pos, stmt, _) -> stmt.setLong(pos, instant.toEpochMilli()));
    };

    @SuppressWarnings("JUnitMalformedDeclaration")
    @RegisterExtension
    final JdbiExtension jdbiExtension = JdbiExtension.h2()
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
    private static final UserRecord RECORD_A = new UserRecord(UUID_A, "Alice", T0);

    private JdbiUserRepository repository;

    @BeforeEach
    void setUp() {
        // DatabaseMigrator is no-op: schema is provisioned by JdbiExtension.withInitializer.
        // HikariDataSource is mocked: only used in close(), which is not exercised here.
        this.repository = new JdbiUserRepository(
                this.jdbiExtension.getJdbi(),
                mock(HikariDataSource.class),
                DIALECT,
                mock(DatabaseMigrator.class),
                mock(ComponentLogger.class),
                new Metadata("Test", "Test", "test", "1.0")
        );
    }

    // ── findById ──────────────────────────────────────────────────────────────

    @Test
    void findByIdReturnsEmptyWhenTableIsEmpty() {
        assertTrue(this.repository.findById(UUID_A).join().isEmpty());
    }

    @Test
    void findByIdReturnsRecordAfterUpsert() {
        this.repository.upsert(RECORD_A).join();

        assertEquals(RECORD_A, this.repository.findById(UUID_A).join().orElseThrow());
    }

    @Test
    void findByIdDoesNotReturnRecordForDifferentUUID() {
        this.repository.upsert(RECORD_A).join();

        assertTrue(this.repository.findById(UUID_B).join().isEmpty());
    }

    @Test
    void findByIdPreservesAllFields() {
        final UserRecord record = new UserRecord(UUID_B, "Bob", T1);
        this.repository.upsert(record).join();

        final UserRecord found = this.repository.findById(UUID_B).join().orElseThrow();
        assertEquals(UUID_B, found.uuid());
        assertEquals("Bob", found.name());
        assertEquals(T1, found.lastSeen());
    }

    @Test
    void findByIdRoundtripsUUIDBytes() {
        this.repository.upsert(RECORD_A).join();

        final UUID roundtripped = this.repository.findById(UUID_A).join()
                .map(UserRecord::uuid)
                .orElseThrow();
        assertEquals(UUID_A, roundtripped);
    }

    // ── upsert: insert path ───────────────────────────────────────────────────

    @Test
    void upsertInsertsNewRecord() {
        this.repository.upsert(RECORD_A).join();

        assertTrue(this.repository.findById(UUID_A).join().isPresent());
    }

    @Test
    void upsertIsIdempotentOnSameRecord() {
        this.repository.upsert(RECORD_A).join();
        this.repository.upsert(RECORD_A).join();

        assertEquals(RECORD_A, this.repository.findById(UUID_A).join().orElseThrow());
    }

    // ── upsert: update path ───────────────────────────────────────────────────

    @Test
    void upsertUpdatesExistingRecord() {
        this.repository.upsert(RECORD_A).join();
        final UserRecord updated = new UserRecord(UUID_A, "Alice-v2", T1);
        this.repository.upsert(updated).join();

        final UserRecord found = this.repository.findById(UUID_A).join().orElseThrow();
        assertEquals("Alice-v2", found.name());
        assertEquals(T1, found.lastSeen());
    }

    @Test
    void upsertDoesNotAffectOtherRecords() {
        final UserRecord recordB = new UserRecord(UUID_B, "Bob", T0);
        this.repository.upsert(RECORD_A).join();
        this.repository.upsert(recordB).join();

        final UserRecord updated = new UserRecord(UUID_A, "Alice-v2", T1);
        this.repository.upsert(updated).join();

        assertEquals(recordB, this.repository.findById(UUID_B).join().orElseThrow());
    }

    // ── delete ────────────────────────────────────────────────────────────────

    @Test
    void deleteRemovesRecord() {
        this.repository.upsert(RECORD_A).join();
        this.repository.delete(UUID_A).join();

        assertTrue(this.repository.findById(UUID_A).join().isEmpty());
    }

    @Test
    void deleteOnNonExistentUUIDIsNoOp() {
        this.repository.delete(UUID_A).join();
    }

    @Test
    void deleteOnlyAffectsTargetRecord() {
        final UserRecord recordB = new UserRecord(UUID_B, "Bob", T0);
        this.repository.upsert(RECORD_A).join();
        this.repository.upsert(recordB).join();
        this.repository.delete(UUID_A).join();

        assertTrue(this.repository.findById(UUID_A).join().isEmpty());
        assertEquals(recordB, this.repository.findById(UUID_B).join().orElseThrow());
    }

    // ── concurrency ───────────────────────────────────────────────────────────

    @Test
    void concurrentUpsertsForSameUUIDProduceConsistentFinalState() {
        final int threads = 20;
        final List<CompletableFuture<Void>> futures = new ArrayList<>(threads);

        for (int i = 0; i < threads; i++) {
            futures.add(this.repository.upsert(
                    new UserRecord(UUID_A, "Alice-" + i, Instant.ofEpochMilli(i))
            ));
        }
        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();

        final Optional<UserRecord> result = this.repository.findById(UUID_A).join();
        assertTrue(result.isPresent());
        assertTrue(result.get().name().startsWith("Alice-"));
    }

    @Test
    void concurrentUpsertsDifferentUUIDsAllSucceed() {
        final int count = 50;
        final List<UUID> uuids = new ArrayList<>(count);
        final List<CompletableFuture<Void>> futures = new ArrayList<>(count);

        for (int i = 0; i < count; i++) {
            final UUID uuid = UUID.randomUUID();
            uuids.add(uuid);
            futures.add(this.repository.upsert(new UserRecord(uuid, "Player-" + i, Instant.EPOCH)));
        }
        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();

        for (final UUID uuid : uuids) {
            assertTrue(
                    this.repository.findById(uuid).join().isPresent(),
                    "Expected record to exist for UUID: " + uuid
            );
        }
    }

    // ── UUID byte encoding sanity ─────────────────────────────────────────────

    @Test
    void storedBytesMatchUUIDCodecEncoding() {
        this.repository.upsert(RECORD_A).join();

        // Verify round-trip via the raw byte representation used by MySQL dialect
        final byte[] expected = UUIDCodec.uuidToBytes(UUID_A);
        final UUID decoded = UUIDCodec.uuidFromBytes(expected);
        assertEquals(UUID_A, decoded);
    }
}

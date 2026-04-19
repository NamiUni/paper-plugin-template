package io.github.namiuni.paperplugintemplate.common.user.json;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.google.gson.GsonBuilder;
import io.github.namiuni.paperplugintemplate.common.Metadata;
import io.github.namiuni.paperplugintemplate.common.user.UserRecord;
import io.github.namiuni.paperplugintemplate.common.utilities.gson.serializations.InstantTypeAdapter;
import io.github.namiuni.paperplugintemplate.common.utilities.gson.serializations.UUIDTypeAdapter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@NullMarked
class JsonUserRepositoryTest {

    private static final UUID UUID_A = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID UUID_B = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UserRecord RECORD_A = new UserRecord(UUID_A, "Alice", Instant.ofEpochMilli(1_000L));
    private static final Metadata METADATA = new Metadata("Test", "Test", "test", "1.0");

    @TempDir
    Path tempDir;

    private JsonUserRepository repository;

    @BeforeEach
    void setUp() {
        final var gson = new GsonBuilder()
                .registerTypeAdapter(Instant.class, InstantTypeAdapter.INSTANCE)
                .registerTypeAdapter(UUID.class, UUIDTypeAdapter.INSTANCE)
                .create();
        this.repository = new JsonUserRepository(this.tempDir, mock(ComponentLogger.class), gson, METADATA);
    }

    @Test
    void findByIdReturnsEmptyWhenNoFileExists() {
        assertTrue(this.repository.findById(UUID_A).join().isEmpty());
    }

    @Test
    void findByIdReturnsRecordAfterUpsert() {
        this.repository.upsert(RECORD_A).join();

        assertEquals(RECORD_A, this.repository.findById(UUID_A).join().orElseThrow());
    }

    @Test
    void findByIdDoesNotReturnRecordFromDifferentUUID() {
        this.repository.upsert(RECORD_A).join();

        assertTrue(this.repository.findById(UUID_B).join().isEmpty());
    }

    @Test
    void upsertCreatesFileOnDisk() {
        this.repository.upsert(RECORD_A).join();

        assertTrue(Files.exists(this.tempDir.resolve("users").resolve(UUID_A + ".json")));
    }

    @Test
    void upsertOverwritesExistingRecord() {
        final UserRecord updated = new UserRecord(UUID_A, "Alice-Updated", Instant.ofEpochMilli(9_999L));

        this.repository.upsert(RECORD_A).join();
        this.repository.upsert(updated).join();

        final UserRecord result = this.repository.findById(UUID_A).join().orElseThrow();
        assertEquals("Alice-Updated", result.name());
        assertEquals(Instant.ofEpochMilli(9_999L), result.lastSeen());
    }

    @Test
    void upsertPreservesAllFields() {
        final UserRecord record = new UserRecord(UUID_B, "Bob", Instant.ofEpochMilli(42_000L));
        this.repository.upsert(record).join();

        final UserRecord result = this.repository.findById(UUID_B).join().orElseThrow();

        assertEquals(UUID_B, result.uuid());
        assertEquals("Bob", result.name());
        assertEquals(Instant.ofEpochMilli(42_000L), result.lastSeen());
    }

    @Test
    void upsertDoesNotCreateTmpFileAfterCompletion() {
        this.repository.upsert(RECORD_A).join();

        final Path usersDir = this.tempDir.resolve("users");
        final boolean tmpExists = usersDir.toFile().listFiles() != null
                && Arrays.stream(Objects.requireNonNull(usersDir.toFile().listFiles()))
                .anyMatch(f -> f.getName().endsWith(".tmp"));

        assertFalse(tmpExists);
    }

    @Test
    void deleteRemovesFileFromDisk() {
        this.repository.upsert(RECORD_A).join();
        this.repository.delete(UUID_A).join();

        assertTrue(this.repository.findById(UUID_A).join().isEmpty());
    }

    @Test
    void deleteOnNonExistentUUIDIsNoOp() {
        this.repository.delete(UUID_A).join();
    }

    @Test
    void deleteOnlyAffectsTargetUUID() {
        final UserRecord recordB = new UserRecord(UUID_B, "Bob", Instant.EPOCH);

        this.repository.upsert(RECORD_A).join();
        this.repository.upsert(recordB).join();
        this.repository.delete(UUID_A).join();

        assertTrue(this.repository.findById(UUID_A).join().isEmpty());
        assertEquals(recordB, this.repository.findById(UUID_B).join().orElseThrow());
    }

    @Test
    void concurrentUpsertsSameUUIDProduceConsistentFinalState() {
        final int threadCount = 20;
        final List<CompletableFuture<Void>> futures = new ArrayList<>(threadCount);

        for (int i = 0; i < threadCount; i++) {
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

        uuids.forEach(uuid ->
                assertTrue(this.repository.findById(uuid).join().isPresent(),
                        "Expected record to exist for UUID: " + uuid)
        );
    }
}

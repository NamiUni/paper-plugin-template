/*
 * PaperPluginTemplate
 *
 * Copyright (c) 2026. Namiu (うにたろう)
 *                     Contributors
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
package io.github.namiuni.paperplugintemplate.common.user.storage.json;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import io.github.namiuni.paperplugintemplate.common.DataDirectory;
import io.github.namiuni.paperplugintemplate.common.user.storage.UserProfile;
import io.github.namiuni.paperplugintemplate.common.user.storage.UserRepository;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.jspecify.annotations.NullMarked;

/// [UserRepository] implementation that stores each user profile as an
/// individual JSON file under `<dataDirectory>/users/<uuid>.json`.
///
/// Suitable only for low-traffic, single-server deployments. Prefer the H2 or
/// MySQL backend for anything beyond that due to per-file I/O overhead and the
/// lack of transactional guarantees across multiple records.
///
/// ## Thread safety
///
/// `synchronized` blocks are avoided entirely to prevent carrier-thread pinning
/// on virtual threads (JEP 491). Instead, a per-UUID [ReentrantReadWriteLock]
/// is maintained in a [ConcurrentHashMap] managed by a Caffeine cache:
///
/// - Multiple concurrent reads for the same UUID proceed in parallel.
/// - A write operation (upsert or delete) for a given UUID excludes all other
///   reads and writes for that UUID.
/// - Operations on different UUIDs are fully independent and never contend.
///
/// The lock entry for a UUID is retained by the Caffeine cache for 10 minutes
/// after last access, bounding memory growth for servers with many transient
/// players.
///
/// ## Atomicity
///
/// Writes are atomic at the file-system level: JSON is first written to a
/// `.tmp` sibling file, then renamed over the target using
/// [StandardCopyOption#ATOMIC_MOVE] where the OS supports it.
///
/// @implNote All I/O runs on a dedicated virtual-thread-per-task executor named
///           `YourPlugin-Json-User-Repo-N`. Callers must never call
///           [CompletableFuture#join()] on the returned futures from the Paper main
///           thread.
@NullMarked
public final class JsonUserRepository implements UserRepository {

    private static final Executor IO_EXECUTOR = Executors.newThreadPerTaskExecutor(
            Thread.ofVirtual().name("YourPlugin-Json-User-Repo-", 0).factory()
    );

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(Instant.class, (JsonSerializer<Instant>) (src, _, _) ->
                    new JsonPrimitive(src.toEpochMilli()))
            .registerTypeAdapter(Instant.class, (JsonDeserializer<Instant>) (json, _, _) ->
                    Instant.ofEpochMilli(json.getAsLong()))
            .registerTypeAdapter(UUID.class, (JsonSerializer<UUID>) (src, _, _) ->
                    new JsonPrimitive(src.toString()))
            .registerTypeAdapter(UUID.class, (JsonDeserializer<UUID>) (json, _, _) ->
                    UUID.fromString(json.getAsString()))
            .create();

    private static final String EXTENSION = ".json";

    private final Path storageDir;
    private final Cache<UUID, ReentrantReadWriteLock> locks;

    /// Constructs a new repository whose files live under
    /// `<dataDirectory>/users/`.
    ///
    /// The `users/` directory is created lazily at [#initialize()] time rather
    /// than here, so construction is safe even before the plugin data directory
    /// exists.
    ///
    /// @param dataDirectory the plugin data directory, injected via
    ///                      [io.github.namiuni.paperplugintemplate.common.DataDirectory]
    public JsonUserRepository(final @DataDirectory Path dataDirectory) {
        this.storageDir = dataDirectory.resolve("users");
        this.locks = Caffeine.newBuilder()
                .expireAfterAccess(10, TimeUnit.MINUTES)
                .build();
    }

    /// {@inheritDoc}
    ///
    /// Creates the `users/` directory tree if it does not already exist.
    ///
    /// @throws UncheckedIOException if the directory cannot be created
    @Override
    public void initialize() {
        try {
            Files.createDirectories(this.storageDir);
        } catch (final IOException exception) {
            throw new UncheckedIOException("Failed to create user data directory", exception);
        }
    }

    /// {@inheritDoc}
    ///
    /// @implNote Acquires the read lock for `uuid` during the file read.
    ///           Multiple concurrent reads for the same UUID proceed in parallel; only
    ///           an active write lock causes a read to wait. The lock is released in a
    ///           `finally` block, ensuring no lock leak.
    @Override
    public CompletableFuture<Optional<UserProfile>> findById(final UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            final ReentrantReadWriteLock.ReadLock lock = this.lockFor(uuid).readLock();
            lock.lock();
            try {
                final Path file = this.fileFor(uuid);
                if (!Files.exists(file)) {
                    return Optional.empty();
                }
                final String json = Files.readString(file);
                if (json.isBlank()) {
                    return Optional.empty();
                }
                return Optional.of(GSON.fromJson(json, UserProfile.class));
            } catch (final IOException exception) {
                throw new UncheckedIOException(
                        "Failed to read user file for UUID: " + uuid, exception);
            } finally {
                lock.unlock();
            }
        }, IO_EXECUTOR);
    }

    /// {@inheritDoc}
    ///
    /// @implNote Acquires the write lock for `uuid` for the duration of to
    ///           write. The file is first written to a `.tmp` sibling, then atomically
    ///           moved to the final path. The lock is released in a `finally` block.
    @Override
    public CompletableFuture<Void> upsert(final UserProfile userProfile) {
        final UUID uuid = userProfile.uuid();
        return CompletableFuture.runAsync(() -> {
            final ReentrantReadWriteLock.WriteLock lock = this.lockFor(uuid).writeLock();
            lock.lock();
            try {
                final Path file = this.fileFor(uuid);
                final Path tmpFile = file.resolveSibling(file.getFileName() + ".tmp");
                Files.writeString(tmpFile, GSON.toJson(userProfile));
                Files.move(tmpFile, file,
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (final IOException exception) {
                throw new UncheckedIOException(
                        "Failed to write user file for UUID: " + uuid, exception);
            } finally {
                lock.unlock();
            }
        }, IO_EXECUTOR);
    }

    /// {@inheritDoc}
    ///
    /// @implNote Acquires the write lock for `uuid`. Uses
    ///           [Files#deleteIfExists] so the operation is a no-op when no file
    ///           exists for the given UUID.
    @Override
    public CompletableFuture<Void> delete(final UUID uuid) {
        return CompletableFuture.runAsync(() -> {
            final ReentrantReadWriteLock.WriteLock lock = this.lockFor(uuid).writeLock();
            lock.lock();
            try {
                Files.deleteIfExists(this.fileFor(uuid));
            } catch (final IOException exception) {
                throw new UncheckedIOException(
                        "Failed to delete user file for UUID: " + uuid, exception);
            } finally {
                lock.unlock();
            }
        }, IO_EXECUTOR);
    }

    private ReentrantReadWriteLock lockFor(final UUID uuid) {
        return this.locks.get(uuid, _ -> new ReentrantReadWriteLock());
    }

    private Path fileFor(final UUID uuid) {
        return this.storageDir.resolve(uuid + EXTENSION);
    }
}

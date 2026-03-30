/*
 * paper-plugin-template
 *
 * Copyright (c) 2026. Namiu/Unitarou
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
package io.github.namiuni.paperplugintemplate.user.storage.json;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import io.github.namiuni.paperplugintemplate.DataDirectory;
import io.github.namiuni.paperplugintemplate.user.storage.UserProfile;
import io.github.namiuni.paperplugintemplate.user.storage.UserRepository;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.jspecify.annotations.NullMarked;

/// [UserRepository] implementation that stores each user as an individual
/// JSON file under `<dataDirectory>/users/<uuid>.json`.
///
/// All I/O is asynchronous and delegated to a virtual-thread executor. This
/// implementation is suitable for low-traffic servers; for anything beyond that,
/// prefer the H2 or MySQL backend.
///
/// ## Thread safety
///
/// `synchronized` blocks are avoided to prevent virtual-thread carrier pinning.
/// Instead, per-user [ReentrantReadWriteLock] instances are maintained in a
/// Caffeine [Cache] with `expireAfterAccess`, which bounds map growth without
/// requiring explicit eviction on [#delete(UUID)]:
///
///   - Multiple concurrent reads for the same UUID proceed in parallel.
///   - Write (upsert or delete) for a given UUID is exclusive against all other
///     reads and writes for that UUID.
///   - Operations on different UUIDs are fully independent.
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

    /// Constructs a new repository whose files will live under
    /// `<dataDirectory>/users/`.
    ///
    /// @param dataDirectory the plugin data directory, injected via [DataDirectory]
    public JsonUserRepository(final @DataDirectory Path dataDirectory) {
        this.storageDir = dataDirectory.resolve("users");

        this.locks = Caffeine.newBuilder()
                .expireAfterAccess(10, TimeUnit.MINUTES)
                .build();
    }

    /// {@inheritDoc}
    ///
    /// Creates the `users/` directory tree if it does not exist.
    @Override
    public void initialize() {
        try {
            Files.createDirectories(this.storageDir);
        } catch (final IOException exception) {
            throw new UncheckedIOException("Failed to create user data directory", exception);
        }
    }

    /// {@inheritDoc}
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
                throw new UncheckedIOException("Failed to read user file for UUID: " + uuid, exception);
            } finally {
                lock.unlock();
            }
        }, IO_EXECUTOR);
    }

    /// {@inheritDoc}
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
                Files.move(tmpFile, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (final IOException exception) {
                throw new UncheckedIOException("Failed to write user file for UUID: " + uuid, exception);
            } finally {
                lock.unlock();
            }
        }, IO_EXECUTOR);
    }

    /// {@inheritDoc}
    @Override
    public CompletableFuture<Void> delete(final UUID uuid) {
        return CompletableFuture.runAsync(() -> {
            final ReentrantReadWriteLock.WriteLock lock = this.lockFor(uuid).writeLock();
            lock.lock();
            try {
                Files.deleteIfExists(this.fileFor(uuid));
            } catch (final IOException exception) {
                throw new UncheckedIOException("Failed to delete user file for UUID: " + uuid, exception);
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

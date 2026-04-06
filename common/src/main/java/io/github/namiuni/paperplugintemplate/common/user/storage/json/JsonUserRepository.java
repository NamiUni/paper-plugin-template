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
import jakarta.inject.Inject;
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
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.jspecify.annotations.NullMarked;

/// [UserRepository] implementation that stores each user profile as an
/// individual JSON file under `<dataDirectory>/users/<uuid>.json`.
///
/// Suitable only for low-traffic, single-server deployments. Prefer the H2 or
/// MySQL backend for anything beyond that.
///
/// ## Thread safety
///
/// `synchronized` blocks are avoided entirely to prevent carrier-thread pinning
/// on virtual threads (JEP 491). A per-UUID [ReentrantReadWriteLock] maintained
/// in a Caffeine cache provides the necessary mutual exclusion.
///
/// ## Atomicity
///
/// Writes use a `.tmp`-then-atomic-rename strategy for crash-safety.
@NullMarked
public final class JsonUserRepository implements UserRepository {

    private static final Executor IO_EXECUTOR = Executors.newThreadPerTaskExecutor(
            Thread.ofVirtual().name("PaperPluginTemplate-Json-User-Pool", 0).factory()
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
    private final ComponentLogger logger;

    /// Constructs a new repository whose files live under
    /// `<dataDirectory>/users/`.
    ///
    /// @param dataDirectory the plugin data directory, injected via [DataDirectory]
    /// @param logger        the component-aware logger
    @Inject
    private JsonUserRepository(
            final @DataDirectory Path dataDirectory,
            final ComponentLogger logger
    ) {
        this.storageDir = dataDirectory.resolve("users");
        this.logger = logger;
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
            this.logger.info("JSON storage directory ready: {}", this.storageDir);
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
                    this.logger.debug("[JSON] findById: no file for {}", uuid);
                    return Optional.empty();
                }
                final String json = Files.readString(file);
                if (json.isBlank()) {
                    this.logger.debug("[JSON] findById: empty file for {}", uuid);
                    return Optional.empty();
                }
                this.logger.debug("[JSON] findById: loaded profile for {}", uuid);
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
                this.logger.debug("[JSON] upsert: wrote profile for {} ({})", uuid, userProfile.name());
            } catch (final IOException exception) {
                throw new UncheckedIOException(
                        "Failed to write user file for UUID: " + uuid, exception);
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
                final boolean deleted = Files.deleteIfExists(this.fileFor(uuid));
                if (deleted) {
                    this.logger.debug("[JSON] delete: removed profile file for {}", uuid);
                } else {
                    this.logger.debug("[JSON] delete: no file to remove for {}", uuid);
                }
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

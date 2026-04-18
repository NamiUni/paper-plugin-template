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
package io.github.namiuni.paperplugintemplate.common.infrastructure.storage.json;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.gson.Gson;
import io.github.namiuni.paperplugintemplate.common.Metadata;
import io.github.namiuni.paperplugintemplate.common.infrastructure.DataDirectory;
import io.github.namiuni.paperplugintemplate.common.infrastructure.storage.UserRecord;
import io.github.namiuni.paperplugintemplate.common.infrastructure.storage.UserRepository;
import jakarta.inject.Inject;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.jspecify.annotations.NullMarked;

@NullMarked
public final class JsonUserRepository implements UserRepository {

    private static final String EXTENSION = ".json";

    private final Path storageDir;
    private final ComponentLogger logger;
    private final Gson gson;

    private final Executor ioExecutor;
    private final Cache<UUID, ReentrantReadWriteLock> locks;

    @Inject
    JsonUserRepository(
            final @DataDirectory Path dataDirectory,
            final ComponentLogger logger,
            final Gson gson,
            final Metadata metadata
    ) {
        this.storageDir = dataDirectory.resolve("users");
        this.logger = logger;
        this.gson = gson;

        this.ioExecutor = Executors.newThreadPerTaskExecutor(
                Thread.ofVirtual().name(metadata.name() + "-Json-User-Pool", 0).factory()
        );

        this.locks = Caffeine.newBuilder()
                .expireAfterAccess(10, TimeUnit.MINUTES)
                .build();

        try {
            Files.createDirectories(this.storageDir);
            this.logger.info("JSON storage directory ready: {}", this.storageDir);
        } catch (final IOException exception) {
            throw new UncheckedIOException("Failed to create player data directory", exception);
        }
    }

    @Override
    public CompletableFuture<Optional<UserRecord>> findById(final UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            final ReentrantReadWriteLock.ReadLock lock = this.lockFor(uuid).readLock();
            lock.lock();
            try {
                final Path file = this.fileFor(uuid);
                if (!Files.exists(file)) {
                    this.logger.debug("[{}] no file for {}", JsonUserRepository.class.getSimpleName(), uuid);
                    return Optional.empty();
                }
                final String json = Files.readString(file);
                if (json.isBlank()) {
                    this.logger.debug("[{}] empty file for {}", JsonUserRepository.class.getSimpleName(), uuid);
                    return Optional.empty();
                }
                this.logger.debug("[{}] loaded profile for {}: {}", JsonUserRepository.class.getSimpleName(), uuid, json);
                return Optional.of(this.gson.fromJson(json, UserRecord.class));
            } catch (final IOException exception) {
                throw new UncheckedIOException("Failed to read player file for UUID: " + uuid, exception);
            } finally {
                lock.unlock();
            }
        }, this.ioExecutor);
    }

    @Override
    public CompletableFuture<Void> upsert(final UserRecord userRecord) {
        final UUID uuid = userRecord.uuid();
        return CompletableFuture.runAsync(() -> {
            final ReentrantReadWriteLock.WriteLock lock = this.lockFor(uuid).writeLock();
            lock.lock();
            try {
                final Path file = this.fileFor(uuid);
                final Path tmpFile = file.resolveSibling(file.getFileName() + ".tmp");
                Files.writeString(tmpFile, this.gson.toJson(userRecord));
                Files.move(tmpFile, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                this.logger.debug("[{}] wrote profile for {} ({})", JsonUserRepository.class.getSimpleName(), uuid, userRecord);
            } catch (final IOException exception) {
                throw new UncheckedIOException("Failed to write player file for UUID: " + uuid, exception);
            } finally {
                lock.unlock();
            }
        }, this.ioExecutor);
    }

    @Override
    public CompletableFuture<Void> delete(final UUID uuid) {
        return CompletableFuture.runAsync(() -> {
            final ReentrantReadWriteLock.WriteLock lock = this.lockFor(uuid).writeLock();
            lock.lock();
            try {
                final boolean deleted = Files.deleteIfExists(this.fileFor(uuid));
                if (deleted) {
                    this.logger.debug("[{}] removed profile file for {}", JsonUserRepository.class.getSimpleName(), uuid);
                } else {
                    this.logger.debug("[{}] no file to remove for {}", JsonUserRepository.class.getSimpleName(), uuid);
                }
            } catch (final IOException exception) {
                throw new UncheckedIOException("Failed to delete player file for UUID: " + uuid, exception);
            } finally {
                lock.unlock();
            }
        }, this.ioExecutor);
    }

    @Override
    public void close() {
        this.logger.debug("[{}] JSON repository closed (no-op).", JsonUserRepository.class.getSimpleName());
    }

    private ReentrantReadWriteLock lockFor(final UUID uuid) {
        return this.locks.get(uuid, _ -> new ReentrantReadWriteLock());
    }

    private Path fileFor(final UUID uuid) {
        return this.storageDir.resolve(uuid + EXTENSION);
    }
}

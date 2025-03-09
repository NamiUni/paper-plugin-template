/*
 * PluginTemplate
 *
 * Copyright (c) 2025. Namiu/Unitarou
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
package com.github.namiuni.plugintemplate.util;

import org.jspecify.annotations.NullMarked;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.stream.Stream;

@NullMarked
public final class MoreFiles {

    private MoreFiles() {

    }

    public static void createDirectoriesIfNotExists(final Path path) throws IOException {
        if (Files.exists(path) && (Files.isDirectory(path) || Files.isSymbolicLink(path))) {
            return;
        }

        try {
            Files.createDirectories(path);
        } catch (final FileAlreadyExistsException ignore) {
            // ignore
        }
    }

    public static void walkAsDirectory(final Path path, final Consumer<Stream<Path>> consumer) throws IOException {
        if (Files.isDirectory(path)) {
            try (final Stream<Path> paths = Files.walk(path)) {
                consumer.accept(paths);
            }
        }

        try (final FileSystem archiveFile = FileSystems.newFileSystem(path, MoreFiles.class.getClassLoader())) {
            final Path archivePath = archiveFile.getRootDirectories().iterator().next();
            try (final Stream<Path> paths = Files.walk(archivePath)) {
                consumer.accept(paths);
            }
        }
    }
}

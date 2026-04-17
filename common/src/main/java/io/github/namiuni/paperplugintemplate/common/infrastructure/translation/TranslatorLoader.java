package io.github.namiuni.paperplugintemplate.common.infrastructure.translation;

import io.github.namiuni.paperplugintemplate.common.Metadata;
import io.github.namiuni.paperplugintemplate.common.infrastructure.DataDirectory;
import io.github.namiuni.paperplugintemplate.common.infrastructure.PluginResource;
import jakarta.inject.Inject;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.translation.MiniMessageTranslationStore;
import net.kyori.adventure.translation.Translator;
import org.intellij.lang.annotations.Subst;
import org.jspecify.annotations.NullMarked;

@NullMarked
final class TranslatorLoader {

    private static final String BASE_NAME = "messages";
    private static final String FILE_SUFFIX = ".properties";

    private final ComponentLogger logger;
    private final MiniMessage miniMessage;
    private final Path translationDir;
    private final Path pluginResource;
    private final net.kyori.adventure.key.Key translationKey;

    @Inject
    private TranslatorLoader(
            final ComponentLogger logger,
            final MiniMessage miniMessage,
            final @DataDirectory Path dataDirectory,
            final @PluginResource Path pluginResource,
            final Metadata metadata
    ) {
        this.logger = logger;
        this.miniMessage = miniMessage;
        this.translationDir = dataDirectory.resolve("translations");
        this.pluginResource = pluginResource;

        try {
            Files.createDirectories(this.translationDir);
        } catch (final IOException exception) {
            throw new UncheckedIOException(exception);
        }

        final @Subst("namespace") String namespace = metadata.namespace();
        this.translationKey = net.kyori.adventure.key.Key.key(namespace, BASE_NAME);
    }

    Translator loadTranslator() {
        this.logger.debug("[{}] Building translation store...", TranslatorLoader.class.getSimpleName());
        final MiniMessageTranslationStore store = MiniMessageTranslationStore.create(this.translationKey, this.miniMessage);
        store.defaultLocale(Locale.ROOT);

        final Set<Locale> diskLocales = this.loadFromDisk(store);
        this.loadFromJar(store, diskLocales);

        this.logger.debug("[{}] Translation store build complete.", TranslatorLoader.class.getSimpleName());
        return store;
    }

    private Set<Locale> loadFromDisk(final MiniMessageTranslationStore store) {
        final Set<Locale> diskLocales = new HashSet<>();
        try (Stream<Path> files = Files.list(this.translationDir)) {
            files.filter(Files::isRegularFile)
                    .filter(file -> isTranslationFile(file.getFileName().toString()))
                    .forEach(file -> {
                        final Locale locale = parseLocale(file.getFileName().toString());
                        store.registerAll(locale, file, false);
                        diskLocales.add(locale);
                        this.logger.debug(
                                "[{}] Loaded from disk: {} ({})",
                                TranslatorLoader.class.getSimpleName(),
                                file.getFileName(),
                                locale
                        );
                    });
        } catch (final IOException exception) {
            throw new UncheckedIOException(exception);
        }

        if (!diskLocales.isEmpty()) {
            this.logger.info("Loaded {} translation file(s) from disk: {}.", diskLocales.size(), diskLocales);
        }
        return diskLocales;
    }

    private void loadFromJar(final MiniMessageTranslationStore store, final Set<Locale> diskLocales) {
        try (FileSystem jar = FileSystems.newFileSystem(this.pluginResource, TranslatorLoader.class.getClassLoader())) {
            final Path root = jar.getRootDirectories().iterator().next();
            try (Stream<Path> paths = Files.walk(root)) {
                paths.filter(Files::isRegularFile)
                        .filter(path -> isTranslationFile(path.getFileName().toString()))
                        .forEach(path -> {
                            final String fileName = path.getFileName().toString();
                            final Locale locale = parseLocale(fileName);

                            final Path targetPath = locale == Locale.ROOT
                                    ? this.translationDir.resolve("messages_en_US.properties")
                                    : this.translationDir.resolve(fileName);

                            if (!diskLocales.contains(locale)) {
                                store.registerAll(locale, path, false);
                                this.logger.debug(
                                        "[{}] Registered from JAR-source: {}",
                                        TranslatorLoader.class.getSimpleName(),
                                        fileName
                                );

                                if (Files.notExists(targetPath)) {
                                    try {
                                        Files.copy(path, targetPath);
                                        this.logger.debug(
                                                "[{}] Exported: {}",
                                                TranslatorLoader.class.getSimpleName(),
                                                fileName
                                        );
                                    } catch (final IOException exception) {
                                        this.logger.error("Failed to copy resource: " + fileName, exception);
                                    }
                                } else {
                                    this.logger.debug(
                                            "[{}] Skipped export: {} (already exists)",
                                            TranslatorLoader.class.getSimpleName(),
                                            fileName
                                    );
                                }
                            }
                        });
            }
        } catch (final IOException exception) {
            this.logger.warn("Could not scan JAR via FileSystem. Skipping resource export.", exception);
        }
    }

    private static boolean isTranslationFile(final String name) {
        return name.startsWith(BASE_NAME) && name.endsWith(FILE_SUFFIX);
    }

    private static Locale parseLocale(final String fileName) {
        if (Objects.equals(BASE_NAME + FILE_SUFFIX, fileName)) {
            return Locale.ROOT;
        }

        final String tag = fileName.substring(BASE_NAME.length() + 1, fileName.length() - FILE_SUFFIX.length());
        final Locale locale = Translator.parseLocale(tag);
        if (locale == null) {
            throw new IllegalArgumentException("Cannot parse locale from translation file name: " + fileName);
        }

        return locale;
    }
}

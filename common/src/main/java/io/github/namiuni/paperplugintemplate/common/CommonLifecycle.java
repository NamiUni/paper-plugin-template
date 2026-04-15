package io.github.namiuni.paperplugintemplate.common;

import io.github.namiuni.paperplugintemplate.api.PluginTemplate;
import io.github.namiuni.paperplugintemplate.api.PluginTemplateProvider;
import io.github.namiuni.paperplugintemplate.common.command.CommandRegistrar;
import io.github.namiuni.paperplugintemplate.common.infrastructure.persistence.UserRepository;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.jspecify.annotations.NullMarked;

@Singleton
@NullMarked
public final class CommonLifecycle {

    private final UserRepository userRepository;
    private final CommandRegistrar commandRegistrar;
    private final PluginTemplate plugin;
    private final ComponentLogger logger;

    @Inject
    private CommonLifecycle(
            final UserRepository userRepository,
            final CommandRegistrar commandRegistrar,
            final PluginTemplate plugin,
            final ComponentLogger logger
    ) {

        this.userRepository = userRepository;
        this.commandRegistrar = commandRegistrar;
        this.plugin = plugin;
        this.logger = logger;
    }

    public void bootstrap() {
        this.commandRegistrar.registerCommands();
        PluginTemplateProvider.register(this.plugin);
    }

    public void enable() {
        this.logger.info("Plugin enabled.");
    }

    public void disable() {
        this.logger.info("Disabling plugin...");
        try {
            this.userRepository.close();
        } catch (final Exception exception) {
            this.logger.error("Failed to close player repository during shutdown.", exception);
        }
        this.logger.info("Plugin disabled.");
    }
}

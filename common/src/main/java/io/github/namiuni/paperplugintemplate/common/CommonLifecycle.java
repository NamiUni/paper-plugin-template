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
package io.github.namiuni.paperplugintemplate.common;

import io.github.namiuni.paperplugintemplate.api.PluginTemplate;
import io.github.namiuni.paperplugintemplate.api.PluginTemplateProvider;
import io.github.namiuni.paperplugintemplate.common.command.CommandRegistrar;
import io.github.namiuni.paperplugintemplate.common.infrastructure.storage.UserRepository;
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
    CommonLifecycle(
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

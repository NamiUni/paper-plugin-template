/*
 * PaperPluginTemplate
 *
 * Copyright (c) 2026. Namiu (うにたろう)
 * Contributors []
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
package io.github.namiuni.paperplugintemplate.common.command.commands;

import io.github.namiuni.paperplugintemplate.common.Metadata;
import io.github.namiuni.paperplugintemplate.common.command.CommandConfig;
import io.github.namiuni.paperplugintemplate.common.command.CommandSource;
import io.github.namiuni.paperplugintemplate.common.command.arguments.ConfigHolderParser;
import io.github.namiuni.paperplugintemplate.common.infrastructure.configuration.ConfigHolder;
import io.github.namiuni.paperplugintemplate.common.permission.PluginPermissions;
import jakarta.inject.Inject;
import java.util.Optional;
import java.util.Set;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.meta.CommandMeta;
import org.incendo.cloud.minecraft.extras.RichDescription;
import org.jspecify.annotations.NullMarked;

@NullMarked
public final class ReloadCommand implements CommandFactory {

    private final ConfigHolderParser configParser;
    private final ConfigHolder<CommandConfig> commandConfig;
    private final Set<ConfigHolder<?>> configurations;
    private final CommandManager<CommandSource> manager;
    private final Metadata metadata;
    private final ComponentLogger logger;

    @Inject
    ReloadCommand(
            final ConfigHolderParser configParser,
            final ConfigHolder<CommandConfig> commandConfig,
            final Set<ConfigHolder<?>> configurations,
            final CommandManager<CommandSource> manager,
            final Metadata metadata,
            final ComponentLogger logger
    ) {
        this.configParser = configParser;
        this.commandConfig = commandConfig;
        this.configurations = configurations;
        this.manager = manager;
        this.metadata = metadata;
        this.logger = logger;
    }

    @Override
    public Command<CommandSource> createCommand() {
        return this.manager.commandBuilder(
                        this.metadata.namespace(),
                        this.commandConfig.get().admin().aliases(),
                        RichDescription.of(this.commandConfig.get().admin().description()),
                        CommandMeta.empty()
                )
                .literal("reload", this.commandConfig.get().admin().reload().aliases().toArray(String[]::new))
                .optional("config", this.configParser)
                .permission(PluginPermissions.COMMAND_RELOAD)
                .commandDescription(RichDescription.richDescription(this.commandConfig.get().admin().reload().description()))
                .handler(this::executes)
                .build();
    }

    private void executes(final CommandContext<CommandSource> context) {
        final Audience sender = context.sender().sender();
        Optional.ofNullable((ConfigHolder<?>) context.getOrDefault("config", null))
                .ifPresentOrElse(
                        config -> {
                            try {
                                config.reload();
                                sender.sendMessage(this.commandConfig.get().admin().reload().messages().get("success"));
                            } catch (final Throwable exception) {
                                this.logger.error("Failed to reload configuration: {}", config.configName(), exception);
                                sender.sendMessage(this.commandConfig.get().admin().reload().messages().get("failure"));
                            }
                        },
                        () -> {
                            for (final var config : this.configurations) {
                                try {
                                    config.reload();
                                } catch (final Throwable exception) {
                                    this.logger.error("Failed to reload configuration: {}", config.configName(), exception);
                                    sender.sendMessage(this.commandConfig.get().admin().reload().messages().get("failure"));
                                }
                            }
                            sender.sendMessage(this.commandConfig.get().admin().reload().messages().get("success"));
                        }
                );
    }
}

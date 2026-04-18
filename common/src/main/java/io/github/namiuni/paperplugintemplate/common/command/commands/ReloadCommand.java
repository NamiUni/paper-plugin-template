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
package io.github.namiuni.paperplugintemplate.common.command.commands;

import io.github.namiuni.paperplugintemplate.common.Metadata;
import io.github.namiuni.paperplugintemplate.common.command.CommandSource;
import io.github.namiuni.paperplugintemplate.common.infrastructure.Reloadable;
import io.github.namiuni.paperplugintemplate.common.infrastructure.configuration.UncheckedConfigurateException;
import io.github.namiuni.paperplugintemplate.common.infrastructure.configuration.configurations.PrimaryConfiguration;
import io.github.namiuni.paperplugintemplate.common.infrastructure.translation.translations.MessageAssembly;
import io.github.namiuni.paperplugintemplate.common.permission.PluginPermissions;
import jakarta.inject.Inject;
import java.io.UncheckedIOException;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import net.kyori.adventure.translation.Translator;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.description.CommandDescription;
import org.incendo.cloud.minecraft.extras.RichDescription;
import org.jspecify.annotations.NullMarked;

@NullMarked
public final class ReloadCommand implements CommandFactory {

    private final Reloadable<PrimaryConfiguration> configHolder;
    private final Reloadable<Translator> translatorHolder;
    private final MessageAssembly messages;
    private final CommandManager<CommandSource> manager;
    private final Metadata metadata;
    private final ComponentLogger logger;

    @Inject
    ReloadCommand(
            final Reloadable<PrimaryConfiguration> configHolder,
            final Reloadable<Translator> translatorHolder,
            final MessageAssembly messages,
            final CommandManager<CommandSource> manager,
            final Metadata metadata,
            final ComponentLogger logger
    ) {
        this.configHolder = configHolder;
        this.translatorHolder = translatorHolder;
        this.messages = messages;
        this.manager = manager;
        this.metadata = metadata;
        this.logger = logger;
    }

    @Override
    public Command<CommandSource> createCommand() {
        return this.manager.commandBuilder(this.metadata.namespace())
                .literal("reload")
                .permission(PluginPermissions.COMMAND_RELOAD)
                .commandDescription(this.description())
                .handler(this::executes)
                .build();
    }

    private void executes(final CommandContext<CommandSource> context) {
        final Audience sender = context.sender().sender();

        try {
            this.configHolder.reload();
            sender.sendMessage(this.messages.configurationReloadSuccess(sender));
        } catch (final UncheckedConfigurateException exception) {
            this.logger.error("Failed to reload configuration", exception);
            sender.sendMessage(this.messages.configurationReloadFailure(sender));
        }

        try {
            this.translatorHolder.reload();
            sender.sendMessage(this.messages.translationReloadSuccess(sender));
        } catch (final UncheckedIOException exception) {
            this.logger.error("Failed to reload translations", exception);
            sender.sendMessage(this.messages.translationReloadFailure(sender));
        }
    }

    private CommandDescription description() {
        return CommandDescription.commandDescription(RichDescription.of(this.messages.commandReloadDescription()));
    }
}

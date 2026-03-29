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
package io.github.namiuni.paperplugintemplate.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.github.namiuni.paperplugintemplate.configuration.ConfigurationHolder;
import io.github.namiuni.paperplugintemplate.configuration.PrimaryConfiguration;
import io.github.namiuni.paperplugintemplate.permission.TemplatePermission;
import io.github.namiuni.paperplugintemplate.translation.TemplateMessages;
import io.github.namiuni.paperplugintemplate.translation.TranslatorHolder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import jakarta.inject.Inject;
import java.io.IOException;
import java.io.UncheckedIOException;
import net.kyori.adventure.translation.GlobalTranslator;
import org.bukkit.command.CommandSender;
import org.jspecify.annotations.NullMarked;
import org.spongepowered.configurate.ConfigurateException;

/// Administration command that exposes plugin management actions to operators.
///
/// Currently, provides a `/template reload` sub-command that hot-reloads
/// both the primary configuration file and the translation files without restarting
/// the server.
///
/// Requires the [TemplatePermission#COMMAND_RELOAD] permission node.
@NullMarked
public final class AdminCommand implements CommandFactory {

    private final ConfigurationHolder<PrimaryConfiguration> configHolder;
    private final TranslatorHolder translatorHolder;
    private final TemplateMessages templateMessages;

    /// Constructs a new `AdminCommand` with all required dependencies.
    ///
    /// @param configHolder     holder for the primary plugin configuration
    /// @param translatorHolder holder for the active Adventure translator
    /// @param templateMessages message provider for localised feedback
    @Inject
    private AdminCommand(
            final ConfigurationHolder<PrimaryConfiguration> configHolder,
            final TranslatorHolder translatorHolder,
            final TemplateMessages templateMessages
    ) {
        this.configHolder = configHolder;
        this.translatorHolder = translatorHolder;
        this.templateMessages = templateMessages;
    }

    /// Builds the `/template` command tree.
    ///
    /// The tree currently contains a single `reload` sub-command that:
    /// - Reloads the primary configuration from disk.
    /// - Replaces the registered translation source in [GlobalTranslator] with a freshly loaded instance.
    /// Both operations report success or failure to the executing sender via
    /// localized messages.
    ///
    /// @return the root [LiteralCommandNode] for the `/template` command
    @Override
    public LiteralCommandNode<CommandSourceStack> command() {
        return Commands.literal("template") // TODO: change the command name
                .then(this.reloadNode())
                .build();
    }

    public LiteralCommandNode<CommandSourceStack> reloadNode() {
        return Commands.literal("reload")
                .requires(source -> source.getSender().hasPermission(TemplatePermission.COMMAND_RELOAD.node()))
                .executes(context -> {
                    final CommandSender sender = context.getSource().getSender();

                    try {
                        this.configHolder.reload();
                        sender.sendMessage(this.templateMessages.configurationReloadSuccess());
                    } catch (final ConfigurateException exception) {
                        sender.sendMessage(this.templateMessages.configurationReloadFailure());
                        throw new UncheckedIOException(exception);
                    }

                    try {
                        final var oldTranslator = this.translatorHolder.get();
                        final var newTranslator = this.translatorHolder.reload();
                        GlobalTranslator.translator().removeSource(oldTranslator);
                        GlobalTranslator.translator().addSource(newTranslator);
                        sender.sendMessage(this.templateMessages.translationReloadSuccess());
                    } catch (final IOException exception) {
                        sender.sendMessage(this.templateMessages.translationReloadFailure());
                        throw new UncheckedIOException(exception);
                    }

                    return Command.SINGLE_SUCCESS;
                })
                .build();
    }
}

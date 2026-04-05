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
package io.github.namiuni.paperplugintemplate.common.command.commands;

import io.github.namiuni.paperplugintemplate.common.command.CommandSource;
import io.github.namiuni.paperplugintemplate.common.configuration.ConfigurationHolder;
import io.github.namiuni.paperplugintemplate.common.configuration.PrimaryConfiguration;
import io.github.namiuni.paperplugintemplate.common.permission.TemplatePermission;
import io.github.namiuni.paperplugintemplate.common.translation.Messages;
import io.github.namiuni.paperplugintemplate.common.translation.TranslatorHolder;
import jakarta.inject.Inject;
import java.io.IOException;
import java.io.UncheckedIOException;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.translation.Translator;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.description.CommandDescription;
import org.incendo.cloud.minecraft.extras.RichDescription;
import org.jspecify.annotations.NullMarked;
import org.spongepowered.configurate.ConfigurateException;

/// [CommandFactory] that contributes the `/template reload` administration
/// command to the command manager.
///
/// Moving this class into the `common` module removes the dependency on
/// Paper's Brigadier API (`CommandSourceStack`, `Commands.literal`) from
/// the command logic. The platform adapter in `minecraft-paper` is
/// responsible solely for constructing the cloud [CommandManager] with the
/// correct [org.incendo.cloud.SenderMapper]; all command semantics live
/// here, independent of the runtime platform.
///
/// ## Registered commands
///
/// | Command | Permission | Effect |
/// |---|---|---|
/// | `/template reload` | [TemplatePermission#COMMAND_RELOAD] | Reloads config and translations |
///
/// ## Thread safety
///
/// [#command] is invoked once on the bootstrap thread. Each command
/// handler executes on cloud's async execution coordinator (a virtual
/// thread), so the reload operations performed inside
/// [#executes] may block briefly on I/O without stalling the
/// Paper main thread.
@NullMarked
public final class ReloadCommand implements CommandFactory {

    private final ConfigurationHolder<PrimaryConfiguration> configHolder;
    private final TranslatorHolder translatorHolder;
    private final Messages messages;
    private final CommandManager<CommandSource> manager;

    /// Constructs a new registrar with its required dependencies.
    ///
    /// @param configHolder     holder for the primary plugin configuration,
    ///                         reloaded on `/template reload`
    /// @param translatorHolder holder for the active Adventure translator,
    ///                         replaced on `/template reload`
    /// @param messages         localized message provider used to send
    ///                         feedback to the command sender
    @Inject
    private ReloadCommand(
            final ConfigurationHolder<PrimaryConfiguration> configHolder,
            final TranslatorHolder translatorHolder,
            final Messages messages,
            final CommandManager<CommandSource> manager
    ) {
        this.configHolder = configHolder;
        this.translatorHolder = translatorHolder;
        this.messages = messages;
        this.manager = manager;
    }

    /// {@inheritDoc}
    @Override
    public Command<CommandSource> command() {
        return this.manager.commandBuilder("template")
                .literal("reload")
                .permission(TemplatePermission.COMMAND_RELOAD.node())
                .commandDescription(this.description())
                .handler(this::executes)
                .build();
    }

    /// Executes the reload sequence: configuration reload followed by
    /// translation reload.
    ///
    /// Each step reports its outcome to the sender via a localized message.
    /// If configuration reload fails, a [ConfigurateException] is wrapped
    /// in an [UncheckedIOException] and re-thrown to cloud's exception
    /// handler after the failure message has been sent. Translation reload
    /// failures follow the same pattern with [IOException].
    ///
    /// The translation swap is performed atomically with respect to
    /// [GlobalTranslator]: the old source is removed before the new one is
    /// added to prevent a window in which neither source is registered.
    ///
    /// @param  context the cloud command context holding the sender
    /// @throws UncheckedIOException if configuration or translation reload
    ///         fails; the exception is propagated to cloud's exception
    ///         handler after feedback has been sent to the sender
    private void executes(final CommandContext<CommandSource> context) {
        final Audience sender = context.sender().sender();

        try {
            this.configHolder.reload();
            sender.sendMessage(this.messages.configurationReloadSuccess());
        } catch (final ConfigurateException exception) {
            sender.sendMessage(this.messages.configurationReloadFailure());
            // ConfigurateException extends IOException; wrap for unchecked propagation.
            throw new UncheckedIOException(exception);
        }

        try {
            final Translator oldTranslator = this.translatorHolder.get();
            final Translator newTranslator = this.translatorHolder.reload();
            GlobalTranslator.translator().removeSource(oldTranslator);
            GlobalTranslator.translator().addSource(newTranslator);
            sender.sendMessage(this.messages.translationReloadSuccess());
        } catch (final IOException exception) {
            sender.sendMessage(this.messages.translationReloadFailure());
            throw new UncheckedIOException(exception);
        }
    }

    private CommandDescription description() {
        return CommandDescription.commandDescription(RichDescription.of(this.messages.commandReloadDescription()));
    }
}

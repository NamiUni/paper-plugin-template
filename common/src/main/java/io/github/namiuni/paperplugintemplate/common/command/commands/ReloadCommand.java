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
import io.github.namiuni.paperplugintemplate.common.infrastructure.configuration.configurations.PrimaryConfiguration;
import io.github.namiuni.paperplugintemplate.common.infrastructure.translation.translations.MessageAssembly;
import io.github.namiuni.paperplugintemplate.common.permission.PluginPermissions;
import jakarta.inject.Inject;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.translation.Translator;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.description.CommandDescription;
import org.incendo.cloud.minecraft.extras.RichDescription;
import org.jspecify.annotations.NullMarked;

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
/// | `/template reload` | [PluginPermissions#COMMAND_RELOAD] | Reloads config and translations |
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

    private final Reloadable<PrimaryConfiguration> configHolder;
    private final Reloadable<Translator> translatorHolder;
    private final MessageAssembly messages;
    private final CommandManager<CommandSource> manager;
    private final Metadata metadata;

    /// Constructs a new registrar with its required dependencies.
    ///
    /// @param configHolder     holder for the primary plugin configuration, reloaded on `/template reload`
    /// @param translatorHolder holder for the active Adventure translator, replaced on `/template reload`
    /// @param messages         localized message provider used to send feedback to the command sender
    /// @param metadata         the plugin metadata
    @Inject
    private ReloadCommand(
            final Reloadable<PrimaryConfiguration> configHolder,
            final Reloadable<Translator> translatorHolder,
            final MessageAssembly messages,
            final CommandManager<CommandSource> manager,
            final Metadata metadata
    ) {
        this.configHolder = configHolder;
        this.translatorHolder = translatorHolder;
        this.messages = messages;
        this.manager = manager;
        this.metadata = metadata;
    }

    /// {@inheritDoc}
    @Override
    public Command<CommandSource> command() {
        return this.manager.commandBuilder(this.metadata.namespace())
                .literal("reload")
                .permission(PluginPermissions.COMMAND_RELOAD)
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
    private void executes(final CommandContext<CommandSource> context) {
        final Audience sender = context.sender().sender();

        this.configHolder.reload();
        sender.sendMessage(this.messages.configurationReloadSuccess(sender));

        this.translatorHolder.reload();
        sender.sendMessage(this.messages.translationReloadSuccess(sender));
    }

    private CommandDescription description() {
        return CommandDescription.commandDescription(RichDescription.of(this.messages.commandReloadDescription()));
    }
}

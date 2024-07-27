/*
 * plugin-template
 *
 * Copyright (c) 2024. Namiu (Unitarou)
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

package com.github.namiuni.plugintemplate.command.commands;

import com.github.namiuni.plugintemplate.command.BaseCommand;
import com.github.namiuni.plugintemplate.config.ConfigManager;
import com.github.namiuni.plugintemplate.message.MessageService;
import com.github.namiuni.plugintemplate.message.TranslationManager;
import com.google.inject.Inject;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.configuration.PluginMeta;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;

import java.util.List;

@DefaultQualifier(NonNull.class)
@SuppressWarnings("UnstableApiUsage")
public final class ReloadCommand implements BaseCommand {

    private final PluginMeta pluginMeta;
    private final ConfigManager configManager;
    private final TranslationManager translationManager;
    private final MessageService messageService;

    @Inject
    public ReloadCommand(
            final PluginMeta pluginMeta,
            final ConfigManager configManager,
            final TranslationManager translationManager,
            final MessageService messageService
    ) {
        this.pluginMeta = pluginMeta;
        this.configManager = configManager;
        this.translationManager = translationManager;
        this.messageService = messageService;
    }

    @Override
    public LiteralCommandNode<CommandSourceStack> create() {
        return Commands.literal(this.pluginMeta.getName().toLowerCase())
                .then(Commands.literal("reload")
                        .requires(context -> context.getSender().hasPermission("plugintemplate.reload"))
                        .executes(context -> {
                            this.configManager.reloadPrimaryConfig();
                            this.translationManager.reloadTranslations();
                            this.messageService.configReloadSuccess(context.getSource().getSender());
                            return Command.SINGLE_SUCCESS;
                        }))
                .build();
    }

    @Override
    public List<String> aliases() {
        return List.of("pt");
    }
}

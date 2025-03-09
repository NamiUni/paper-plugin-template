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
package com.github.namiuni.plugintemplate.command.commands;

import com.github.namiuni.plugintemplate.configuration.ConfigurationManager;
import com.github.namiuni.plugintemplate.translation.TranslationService;
import com.github.namiuni.plugintemplate.translation.TranslationSource;
import com.google.inject.Inject;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.jspecify.annotations.NullMarked;

@NullMarked
@SuppressWarnings("UnstableApiUsage")
public final class ReloadCommand implements PluginCommand {

    private final ConfigurationManager configManager;
    private final TranslationSource translationSource;
    private final TranslationService translationService;

    @Inject
    private ReloadCommand(
            final ConfigurationManager configManager,
            final TranslationSource translationSource,
            final TranslationService translationService
    ) {
        this.configManager = configManager;
        this.translationSource = translationSource;
        this.translationService = translationService;
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> node() {
        return Commands.literal("reload")
                .requires(context -> context.getSender().hasPermission("plugin.reload")) //TODO change
                .executes(context -> {
                    this.configManager.loadConfigurations();
                    this.translationSource.loadTranslations();
                    this.translationService.configReloadSuccess(context.getSource().getSender());
                    return Command.SINGLE_SUCCESS;
                });
    }
}

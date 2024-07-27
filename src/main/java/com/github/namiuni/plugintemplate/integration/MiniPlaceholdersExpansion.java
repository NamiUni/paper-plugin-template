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

package com.github.namiuni.plugintemplate.integration;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.github.miniplaceholders.api.Expansion;
import io.papermc.paper.plugin.configuration.PluginMeta;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.bukkit.Bukkit;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;

@Singleton
@SuppressWarnings("UnstableApiUsage")
@DefaultQualifier(NonNull.class)
public final class MiniPlaceholdersExpansion {

    private final PluginMeta pluginMeta;
    private final ComponentLogger logger;

    @Inject
    public MiniPlaceholdersExpansion(
            final PluginMeta pluginMeta,
            final ComponentLogger logger
    ) {
        this.pluginMeta = pluginMeta;
        this.logger = logger;
    }

    public static boolean miniPlaceholdersEnabled() {
        return Bukkit.getPluginManager().isPluginEnabled("MiniPlaceholders");
    }

    // You can register a placeholder
    public void registerExpansion() {
        if (miniPlaceholdersEnabled()) {
            final var expansion = Expansion.builder(pluginMeta.getName())
//                    .audiencePlaceholder("your_audience_placeholder", (audience, queue, ctx) -> )
//                    .globalPlaceholder("your_global_placeholder", ((argumentQueue, context) -> ))
                    .build();
            expansion.register();
            this.logger.info("Register extensions to MiniPlaceholders");
        } else {
            this.logger.warn("MiniPlaceholders is not installed. Skip placeholder registration.");
        }
    }
}

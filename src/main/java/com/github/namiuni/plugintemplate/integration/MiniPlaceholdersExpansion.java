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
package com.github.namiuni.plugintemplate.integration;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.github.miniplaceholders.api.Expansion;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.bukkit.Bukkit;
import org.jspecify.annotations.NullMarked;

@Singleton
@NullMarked
public final class MiniPlaceholdersExpansion {

    private static final String PLUGIN_NAME = "PluginName"; // TODO: change

    private final ComponentLogger logger;

    @Inject
    public MiniPlaceholdersExpansion(final ComponentLogger logger) {
        this.logger = logger;
    }

    public static boolean miniPlaceholdersLoaded() {
        return Bukkit.getPluginManager().isPluginEnabled("MiniPlaceholders");
    }

    public void registerExpansion() {
        if (miniPlaceholdersLoaded()) {
            final var expansion = Expansion.builder(PLUGIN_NAME)
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

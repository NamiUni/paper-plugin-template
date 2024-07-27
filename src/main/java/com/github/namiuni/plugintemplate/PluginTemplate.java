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

package com.github.namiuni.plugintemplate;

import com.github.namiuni.plugintemplate.integration.MiniPlaceholdersExpansion;
import com.google.inject.Inject;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;

import java.util.Set;

@DefaultQualifier(NonNull.class)
// ToDo: Rename class to plugin name
public final class PluginTemplate extends JavaPlugin {

    private final MiniPlaceholdersExpansion miniPlaceholdersExpansion;
    private final Set<Listener> listeners;

    @Inject
    public PluginTemplate(
            final MiniPlaceholdersExpansion miniPlaceholdersExpansion,
            final Set<Listener> listeners
    ) {
        this.miniPlaceholdersExpansion = miniPlaceholdersExpansion;
        this.listeners = listeners;
    }

    @Override
    public void onEnable() {

        // Register extensions to MiniPlaceholders
//        this.miniPlaceholdersExpansion.registerExpansion();

        // Register listener classes
        this.listeners.forEach(listener -> this.getServer().getPluginManager().registerEvents(listener, this));
    }
}

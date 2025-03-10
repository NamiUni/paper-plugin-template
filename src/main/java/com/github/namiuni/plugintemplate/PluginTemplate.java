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
package com.github.namiuni.plugintemplate;

import com.google.inject.Inject;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.NullMarked;

import java.util.Set;

@NullMarked
public final class PluginTemplate extends JavaPlugin { // TODO: rename

//    private final MiniPlaceholdersExpansion miniPlaceholdersExpansion;
    private final Set<Listener> listeners;

    @Inject
    private PluginTemplate(
//            final MiniPlaceholdersExpansion miniPlaceholdersExpansion,
            final Set<Listener> listeners
    ) {
//        this.miniPlaceholdersExpansion = miniPlaceholdersExpansion;
        this.listeners = listeners;
    }

    @Override
    public void onEnable() {

        // Register extensions to MiniPlaceholders
//        this.miniPlaceholdersExpansion.registerExpansion();

        // Register listener classes
        this.listeners.forEach(listener ->
                this.getServer().getPluginManager().registerEvents(listener, this));
    }
}

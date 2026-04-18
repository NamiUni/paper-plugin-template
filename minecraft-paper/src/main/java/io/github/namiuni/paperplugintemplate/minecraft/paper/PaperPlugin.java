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
package io.github.namiuni.paperplugintemplate.minecraft.paper;

import io.github.namiuni.paperplugintemplate.common.CommonLifecycle;
import io.github.namiuni.paperplugintemplate.common.infrastructure.configuration.configurations.PrimaryConfiguration;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import java.util.Set;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.CustomChart;
import org.bstats.charts.SimplePie;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.NullMarked;

@NullMarked
@SuppressWarnings("unused")
public final class PaperPlugin extends JavaPlugin {

    private static final int BSTATS_PLUGIN_ID = 30597;

    private final CommonLifecycle commonLifecycle;
    private final Set<Listener> listeners;
    private final Provider<PrimaryConfiguration> primaryConfig;

    @Inject
    PaperPlugin(
            final CommonLifecycle commonLifecycle,
            final Set<Listener> listeners,
            final Provider<PrimaryConfiguration> primaryConfig
    ) {
        this.commonLifecycle = commonLifecycle;
        this.listeners = listeners;
        this.primaryConfig = primaryConfig;
    }

    @Override
    public void onEnable() {
        this.listeners.forEach(listener -> this.getServer().getPluginManager().registerEvents(listener, this));

        final Metrics metrics = new Metrics(this, BSTATS_PLUGIN_ID);
        final CustomChart chart = new SimplePie(
                "user_storage_type",
                () -> this.primaryConfig.get().storage().type().name()
        );
        metrics.addCustomChart(chart);
        this.commonLifecycle.enable();
    }

    @Override
    public void onDisable() {
        this.commonLifecycle.disable();
    }
}

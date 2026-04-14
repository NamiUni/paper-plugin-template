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

import io.github.namiuni.paperplugintemplate.common.infrastructure.configuration.configurations.PrimaryConfiguration;
import io.github.namiuni.paperplugintemplate.common.infrastructure.persistence.UserRepository;
import io.github.namiuni.paperplugintemplate.minecraft.paper.listeners.UserSessionHandler;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.CustomChart;
import org.bstats.charts.SimplePie;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.NullMarked;

@NullMarked
@SuppressWarnings("unused")
public final class PaperPlugin extends JavaPlugin {

    private static final int BSTATS_PLUGIN_ID = 30597;

    private final UserSessionHandler sessionHandler;
    private final UserRepository userRepository;
    private final ComponentLogger logger;
    private final Provider<PrimaryConfiguration> primaryConfig;

    @Inject
    private PaperPlugin(
            final UserSessionHandler sessionHandler,
            final UserRepository userRepository,
            final ComponentLogger logger,
            final Provider<PrimaryConfiguration> primaryConfig
    ) {
        this.sessionHandler = sessionHandler;
        this.userRepository = userRepository;
        this.logger = logger;
        this.primaryConfig = primaryConfig;
    }

    @Override
    public void onEnable() {
        this.getServer().getPluginManager().registerEvents(this.sessionHandler, this);

        final Metrics metrics = new Metrics(this, BSTATS_PLUGIN_ID);
        final CustomChart chart = new SimplePie(
                "user_storage_type",
                () -> this.primaryConfig.get().storage().type().name()
        );
        metrics.addCustomChart(chart);

        this.logger.info("Plugin enabled.");
    }

    @Override
    public void onDisable() {
        this.logger.info("Disabling plugin...");
        try {
            this.userRepository.close();
        } catch (final Exception exception) {
            this.logger.error("Failed to close user repository during shutdown", exception);
        }
        this.logger.info("Plugin disabled.");
    }
}

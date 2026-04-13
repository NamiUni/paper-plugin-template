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
import io.github.namiuni.paperplugintemplate.common.infrastructure.persistence.sql.JdbiUserRepository;
import io.github.namiuni.paperplugintemplate.minecraft.paper.listeners.UserSessionHandler;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.NullMarked;

/// Main plugin class for the template plugin.
///
/// Instantiated by the Guice injector inside [PaperBootstrap] rather
/// than directly by the Paper framework. All dependencies are supplied via
/// constructor injection; no static accessors or service-locator calls are
/// needed.
@NullMarked
public final class PaperPlugin extends JavaPlugin {

    private final UserSessionHandler sessionHandler;
    private final UserRepository userRepository;
    private final ComponentLogger logger;
    private final Provider<PrimaryConfiguration> primaryConfig;

    /// Constructs a new [PaperPlugin] instance.
    ///
    /// This constructor is invoked exclusively by the Guice injector.
    ///
    /// @param sessionHandler the event listener that drives user data loading and saving
    /// @param userRepository    the active storage backend; closed on [#onDisable()]
    /// @param logger            the component-aware logger
    /// @param primaryConfig     the primary config
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

    /// {@inheritDoc}
    ///
    /// Registers event listeners. Everything else (commands, translations)
    /// is already set up by [PaperBootstrap] before this method is
    /// called.
    @Override
    public void onEnable() {
        this.getServer().getPluginManager().registerEvents(this.sessionHandler, this);

        final int pluginId = 30597;
        final Metrics metrics = new Metrics(this, pluginId);

        metrics.addCustomChart(
                new SimplePie("user_storage_type", () -> this.primaryConfig.get().storage().type().name())
        );

        this.logger.info("Plugin enabled.");
    }

    /// {@inheritDoc}
    ///
    /// Closes the [UserRepository] to release connection pool resources
    /// (HikariCP threads and JDBC connections, or file handles) before the
    /// server shuts down.
    @Override
    public void onDisable() {
        this.logger.info("Disabling plugin...");
        if (this.userRepository instanceof final JdbiUserRepository jdbiUserRepository) {
            jdbiUserRepository.close();
        }

        this.logger.info("Plugin disabled.");
    }
}

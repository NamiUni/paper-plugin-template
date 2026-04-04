/*
 * PaperPluginTemplate
 *
 * Copyright (c) 2026. Namiu (うにたろう)
 *                     Contributors
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

import io.github.namiuni.paperplugintemplate.minecraft.paper.listener.PaperEventHandler;
import io.github.namiuni.paperplugintemplate.common.user.storage.UserRepository;
import io.github.namiuni.paperplugintemplate.common.user.storage.sql.JdbiUserRepository;
import jakarta.inject.Inject;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.NullMarked;

/// Main plugin class for the template plugin.
///
/// Instantiated by the Guice injector inside [PluginBootstrapImpl] rather
/// than directly by the Paper framework. All dependencies are supplied via
/// constructor injection; no static accessors or service-locator calls are
/// needed.
@NullMarked
public final class JavaPluginImpl extends JavaPlugin {

    private final PaperEventHandler paperEventHandler;
    private final UserRepository userRepository;

    /// Constructs a new [JavaPluginImpl] instance.
    ///
    /// This constructor is invoked exclusively by the Guice injector.
    ///
    /// @param paperEventHandler the event listener that drives user data
    ///                          loading and saving
    /// @param userRepository    the active storage backend; closed on
    ///                          [#onDisable()]
    @Inject
    private JavaPluginImpl(
            final PaperEventHandler paperEventHandler,
            final UserRepository userRepository
    ) {
        this.paperEventHandler = paperEventHandler;
        this.userRepository = userRepository;
    }

    /// {@inheritDoc}
    ///
    /// Registers event listeners. Everything else (commands, translations)
    /// is already set up by [PluginBootstrapImpl] before this method is
    /// called.
    @Override
    public void onEnable() {
        this.getServer().getPluginManager().registerEvents(this.paperEventHandler, this);
    }

    /// {@inheritDoc}
    ///
    /// Closes the [UserRepository] to release connection pool resources
    /// (HikariCP threads and JDBC connections, or file handles) before the
    /// server shuts down.
    @Override
    public void onDisable() {
        if (this.userRepository instanceof final JdbiUserRepository jdbiUserRepository) {
            jdbiUserRepository.close();
        }
    }
}

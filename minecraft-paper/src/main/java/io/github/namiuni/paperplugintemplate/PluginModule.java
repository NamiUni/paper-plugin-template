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
package io.github.namiuni.paperplugintemplate;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import io.github.namiuni.paperplugintemplate.commands.AdminCommand;
import io.github.namiuni.paperplugintemplate.commands.CommandFactory;
import io.github.namiuni.paperplugintemplate.common.DataDirectory;
import io.github.namiuni.paperplugintemplate.common.configuration.ConfigurationHolder;
import io.github.namiuni.paperplugintemplate.common.configuration.PrimaryConfiguration;
import io.github.namiuni.paperplugintemplate.listener.PaperEventHandler;
import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import java.nio.file.Path;
import java.util.function.Supplier;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.NullMarked;

/// Root Guice module for the template plugin.
///
/// Binds all core services, configuration, messages, commands, event
/// listeners, and the storage backend in one place. The correct storage
/// backend is selected at construction time based on the preloaded
/// [PrimaryConfiguration.StorageConfig], so no conditional logic is
/// needed at injection time.
///
/// ## Thread safety
///
/// This class carries no mutable state after construction. Guice modules
/// are configured on a single thread during injector creation; once the
/// injector is built this module instance is no longer used and may be
/// safely discarded.
@NullMarked
@SuppressWarnings({"UnstableApiUsage", "unused"})
public final class PluginModule extends AbstractModule {

    private final ComponentLogger logger;
    private final Path dataDirectory;

    /// Constructs a new `PluginModule`.
    ///
    /// @param context the Paper bootstrap context
    PluginModule(final BootstrapContext context) {
        this.logger = context.getLogger();
        this.dataDirectory = context.getDataDirectory();
    }

    /// {@inheritDoc}
    @Override
    protected void configure() {
        this.bind(ComponentLogger.class).toInstance(this.logger);
        this.bind(Path.class).annotatedWith(DataDirectory.class).toInstance(this.dataDirectory);
        this.bind(new TypeLiteral<Supplier<PrimaryConfiguration>>() { })
                .to(new TypeLiteral<ConfigurationHolder<PrimaryConfiguration>>() { });
        this.bind(JavaPlugin.class).to(JavaPluginImpl.class).in(Scopes.SINGLETON);
        this.bind(PaperEventHandler.class).in(Scopes.SINGLETON);

        this.bindCommands();
    }

    /// Registers all [CommandFactory] implementations into a Guice
    /// [com.google.inject.multibindings.Multibinder].
    ///
    /// [CommandFactory] implementations added here are automatically
    /// registered with the Paper command system during bootstrap.
    private void bindCommands() {
        final Multibinder<CommandFactory> commands = Multibinder.newSetBinder(this.binder(), CommandFactory.class);
        commands.addBinding().to(AdminCommand.class).in(Scopes.SINGLETON);
    }
}

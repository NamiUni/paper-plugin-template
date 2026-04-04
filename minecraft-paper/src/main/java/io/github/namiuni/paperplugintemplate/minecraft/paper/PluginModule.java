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

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import io.github.namiuni.paperplugintemplate.api.user.PluginTemplateUserService;
import io.github.namiuni.paperplugintemplate.common.DataDirectory;
import io.github.namiuni.paperplugintemplate.common.command.CommandSource;
import io.github.namiuni.paperplugintemplate.common.command.commands.ReloadCommand;
import io.github.namiuni.paperplugintemplate.common.command.commands.CommandFactory;
import io.github.namiuni.paperplugintemplate.common.command.commands.HelpCommand;
import io.github.namiuni.paperplugintemplate.common.configuration.ConfigurationHolder;
import io.github.namiuni.paperplugintemplate.common.configuration.PrimaryConfiguration;
import io.github.namiuni.paperplugintemplate.common.user.UserFactory;
import io.github.namiuni.paperplugintemplate.minecraft.paper.command.PaperCommandSource;
import io.github.namiuni.paperplugintemplate.minecraft.paper.listener.PaperEventHandler;
import io.github.namiuni.paperplugintemplate.minecraft.paper.user.PaperUserFactory;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import java.nio.file.Path;
import java.util.function.Supplier;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.bukkit.plugin.java.JavaPlugin;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.SenderMapper;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.paper.PaperCommandManager;
import org.jspecify.annotations.NullMarked;

/// Root Guice module for the `minecraft-paper` platform module.
///
/// Binds all Paper-specific singletons — logger, data directory, plugin
/// instance, event handler, and user factory — and initializes the
/// [CommandFactory] [Multibinder] so that [CommonModule] and any future
/// platform-specific modules can safely add their own command registrars
/// without encountering an uninitialized binder.
///
/// ## Command binding convention
///
/// The [CommandFactory] multibinder is created here and populated with:
///
/// - [ReloadCommand]: the `/template reload` administration command.
/// - [HelpCommand]: the `/template help [query]` help browser.
///
/// Platform-specific commands may be added here by calling
/// `commands.addBinding().to(MyPlatformCommand.class)`.
///
/// ## Thread safety
///
/// This class carries no mutable state after construction. Guice modules
/// are configured on a single thread during injector creation; once the
/// injector is built, this module instance is no longer used and may be
/// safely discarded.
@NullMarked
@SuppressWarnings({"UnstableApiUsage", "unused"})
final class PluginModule extends AbstractModule {

    private final BootstrapContext context;

    /// Constructs a new `PluginModule` from the Paper bootstrap context.
    ///
    /// @param context the Paper bootstrap context
    PluginModule(final BootstrapContext context) {
        this.context = context;
    }

    @Provides
    @Singleton
    private CommandManager<CommandSource> commandManager(final PluginTemplateUserService userService) {
        final SenderMapper<CommandSourceStack, CommandSource> senderMapper = SenderMapper.create(
                paperSource -> new PaperCommandSource(paperSource, userService),
                pluginSource -> ((PaperCommandSource) pluginSource).paperSource()
        );

        return PaperCommandManager
                .builder(senderMapper)
                .executionCoordinator(ExecutionCoordinator.asyncCoordinator())
                .buildBootstrapped(context);
    }

    /// {@inheritDoc}
    @Override
    protected void configure() {
        this.bind(ComponentLogger.class).toInstance(this.context.getLogger());
        this.bind(Path.class).annotatedWith(DataDirectory.class).toInstance(this.context.getDataDirectory());
        this.bind(new TypeLiteral<Supplier<PrimaryConfiguration>>() { })
                .to(new TypeLiteral<ConfigurationHolder<PrimaryConfiguration>>() { });
        this.bind(JavaPlugin.class).to(JavaPluginImpl.class).in(Scopes.SINGLETON);
        this.bind(PaperEventHandler.class).in(Scopes.SINGLETON);
        this.bind(UserFactory.class).to(PaperUserFactory.class).in(Scopes.SINGLETON);
    }
}

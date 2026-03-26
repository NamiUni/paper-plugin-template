/*
 * paper-plugin-template
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
package io.github.namiuni.paperplugintemplate;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import io.github.namiuni.paperplugintemplate.commands.CommandFactory;
import io.github.namiuni.paperplugintemplate.guice.TemplateModule;
import io.github.namiuni.paperplugintemplate.translation.TranslatorHolder;
import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.bootstrap.PluginProviderContext;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import java.util.Set;
import net.kyori.adventure.translation.GlobalTranslator;
import org.bukkit.plugin.java.JavaPlugin;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.jspecify.annotations.NullMarked;

/// Bootstrap entry point for the template plugin.
///
/// Responsible for creating the Guice [Injector], registering Brigadier commands
/// via the Paper lifecycle API, and initializing Adventure's [GlobalTranslator] with
/// the plugin's translation sources before the server fully starts.
@NullMarked
@SuppressWarnings({"UnstableApiUsage", "unused"})
public final class TemplateBootstrap implements PluginBootstrap {

    private @MonotonicNonNull Injector injector;

    /// Bootstraps the plugin by creating the Guice injector, registering commands,
    /// and initializing translations.
    ///
    /// @param context the bootstrap context provided by the Paper server
    @Override
    public void bootstrap(final BootstrapContext context) {
        this.injector = Guice.createInjector(new TemplateModule(context));
        this.initialize();

        context.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            final Set<CommandFactory> commands = this.injector.getInstance(Key.get(new TypeLiteral<>() {
            }));
            commands.forEach(command -> event.registrar().register(
                    command.command(),
                    command.description(),
                    command.aliases())
            );
        });
    }

    /// Creates the plugin instance via the Guice injector.
    ///
    /// @param context the plugin provider context provided by the Paper server
    /// @return the fully-injected [JavaPlugin] instance
    @Override
    public JavaPlugin createPlugin(final PluginProviderContext context) {
        return this.injector.getInstance(JavaPlugin.class);
    }

    /// Performs one-time initialization tasks that must run before the server starts.
    ///
    /// Currently registers the plugin's [net.kyori.adventure.translation.Translator]
    /// with Adventure's [GlobalTranslator].
    private void initialize() {
        final TranslatorHolder translatorHolder = this.injector.getInstance(TranslatorHolder.class);
        GlobalTranslator.translator().addSource(translatorHolder.get());
    }
}

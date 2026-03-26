/*
 * paper-plugin-template
 *
 * Copyright (c) 2026. Namiu/Unitarou
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
package io.github.namiuni.paperplugintemplate.guice;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import io.github.namiuni.kotonoha.translatable.message.KotonohaMessage;
import io.github.namiuni.kotonoha.translatable.message.configuration.FormatTypes;
import io.github.namiuni.kotonoha.translatable.message.policy.argument.TranslationArgumentAdaptationPolicy;
import io.github.namiuni.kotonoha.translatable.message.policy.argument.tag.TagNameResolver;
import io.github.namiuni.kotonoha.translatable.message.utility.TranslationArgumentAdapter;
import io.github.namiuni.paperplugintemplate.TemplatePlugin;
import io.github.namiuni.paperplugintemplate.commands.AdminCommand;
import io.github.namiuni.paperplugintemplate.commands.CommandFactory;
import io.github.namiuni.paperplugintemplate.configuration.ConfigurationHolder;
import io.github.namiuni.paperplugintemplate.configuration.ConfigurationLoader;
import io.github.namiuni.paperplugintemplate.configuration.PrimaryConfiguration;
import io.github.namiuni.paperplugintemplate.translation.TemplateMessages;
import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import jakarta.inject.Singleton;
import java.nio.file.Path;
import java.util.function.Supplier;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.NullMarked;

/// Root Guice module for the template plugin.
///
/// Binds all core services, configuration loaders, message proxies, and command
/// factories. Instances are extracted from the [BootstrapContext] at construction
/// time so that they remain available as singletons throughout the plugin's lifetime.
@NullMarked
@SuppressWarnings({"UnstableApiUsage", "unused"})
public final class TemplateModule extends AbstractModule {

    private final ComponentLogger logger;
    private final Path dataDirectory;

    /// Creates a new `TemplateModule` backed by the given bootstrap context.
    ///
    /// @param context the Paper bootstrap context from which the logger and data
    ///                directory are obtained
    public TemplateModule(final BootstrapContext context) {
        this.logger = context.getLogger();
        this.dataDirectory = context.getDataDirectory();
    }

    /// Provides a singleton [ConfigurationLoader] for the [PrimaryConfiguration].
    ///
    /// @param dataDirectory the plugin's data directory, injected via [DataDirectory]
    /// @return a fully constructed configuration loader
    @Provides
    @Singleton
    private ConfigurationLoader<PrimaryConfiguration> primaryConfigLoader(final @DataDirectory Path dataDirectory) {
        return new ConfigurationLoader<>(
                PrimaryConfiguration.class,
                PrimaryConfiguration.DEFAULT,
                dataDirectory
        );
    }

    /// Provides a singleton [TemplateMessages] proxy backed by Kotonoha's
    /// MiniMessage-based translation engine.
    ///
    /// @return a proxied implementation of [TemplateMessages]
    @Provides
    @Singleton
    private TemplateMessages translations() {
        final var argumentPolicy = TranslationArgumentAdaptationPolicy.miniMessage(
                TranslationArgumentAdapter.standard(),
                TagNameResolver.annotationOrParameterNameResolver()
        );
        final var config = FormatTypes.MINI_MESSAGE.withArgumentPolicy(argumentPolicy);

        return KotonohaMessage.createProxy(TemplateMessages.class, config);
    }

    /// {@inheritDoc}
    @Override
    protected void configure() {
        this.bind(ComponentLogger.class).toInstance(this.logger);
        this.bind(Path.class).annotatedWith(DataDirectory.class).toInstance(this.dataDirectory);
        this.bind(new TypeLiteral<Supplier<PrimaryConfiguration>>() {
                })
                .to(new TypeLiteral<ConfigurationHolder<PrimaryConfiguration>>() {
                });
        this.bind(JavaPlugin.class).to(TemplatePlugin.class).in(Scopes.SINGLETON);
        this.bindCommands();
    }

    /// Registers all [CommandFactory] implementations into a Guice [Multibinder].
    ///
    /// Add new command factories here to have them automatically registered with
    /// the Paper command system during bootstrap.
    private void bindCommands() {
        final Multibinder<CommandFactory> commands = Multibinder.newSetBinder(this.binder(), CommandFactory.class);
        commands.addBinding().to(AdminCommand.class).in(Scopes.SINGLETON);
    }
}

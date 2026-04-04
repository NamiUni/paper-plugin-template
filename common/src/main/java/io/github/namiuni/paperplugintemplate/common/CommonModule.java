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
package io.github.namiuni.paperplugintemplate.common;

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
import io.github.namiuni.paperplugintemplate.api.user.PluginTemplateUserService;
import io.github.namiuni.paperplugintemplate.common.command.commands.CommandFactory;
import io.github.namiuni.paperplugintemplate.common.command.commands.HelpCommand;
import io.github.namiuni.paperplugintemplate.common.command.commands.ReloadCommand;
import io.github.namiuni.paperplugintemplate.common.configuration.ConfigurationHolder;
import io.github.namiuni.paperplugintemplate.common.configuration.ConfigurationLoader;
import io.github.namiuni.paperplugintemplate.common.configuration.PrimaryConfiguration;
import io.github.namiuni.paperplugintemplate.common.translation.Messages;
import io.github.namiuni.paperplugintemplate.common.user.PluginTemplateUserServiceInternal;
import jakarta.inject.Singleton;
import java.nio.file.Path;
import java.util.function.Supplier;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.jspecify.annotations.NullMarked;

/// Guice module that binds the application-layer services shared across all
/// platform implementations.
///
/// Configures the following singleton bindings:
///
/// - [ConfigurationLoader] for [PrimaryConfiguration]: reads and writes
///   the primary YAML configuration file.
/// - [Messages] proxy backed by Kotonoha: provides type-safe, locale-aware
///   access to all plugin messages.
/// - [PluginTemplateUserService] → [PluginTemplateUserServiceInternal]:
///   routes the public user-service API to its internal implementation.
///
/// ## Thread safety
///
/// This class carries no mutable state after construction. Guice modules are
/// configured on a single thread during injector creation; once the injector
/// is built, this module instance is no longer used and may be safely
/// discarded.
@NullMarked
@SuppressWarnings("unused")
public final class CommonModule extends AbstractModule {

    private final ComponentLogger logger;
    private final Path dataDirectory;

    /// Constructs a new `CommonModule`.
    public CommonModule(
            final ComponentLogger logger,
            final Path dataDirectory
    ) {
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    /// Provides a singleton [ConfigurationLoader] for [PrimaryConfiguration].
    ///
    /// @param dataDirectory the plugin data directory
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

    /// Provides a singleton [Messages] proxy backed by Kotonoha.
    ///
    /// @return a proxied implementation of [Messages]
    @Provides
    @Singleton
    private Messages translations() {
        final var argumentPolicy = TranslationArgumentAdaptationPolicy.miniMessage(
                TranslationArgumentAdapter.standard(),
                TagNameResolver.annotationOrParameterNameResolver()
        );
        final var config = FormatTypes.MINI_MESSAGE.withArgumentPolicy(argumentPolicy);
        return KotonohaMessage.createProxy(Messages.class, config);
    }

    @Override
    protected void configure() {
        this.bind(ComponentLogger.class).toInstance(this.logger);
        this.bind(Path.class).annotatedWith(DataDirectory.class).toInstance(this.dataDirectory);
        this.bind(PluginTemplateUserService.class).to(PluginTemplateUserServiceInternal.class).in(Scopes.SINGLETON);
        this.bind(new TypeLiteral<Supplier<PrimaryConfiguration>>() { })
                .to(new TypeLiteral<ConfigurationHolder<PrimaryConfiguration>>() { });

        this.bindCommands();
    }

    private void bindCommands() {
        final Multibinder<CommandFactory> commands = Multibinder.newSetBinder(this.binder(), CommandFactory.class);
        commands.addBinding().to(ReloadCommand.class).in(Scopes.SINGLETON);
        commands.addBinding().to(HelpCommand.class).in(Scopes.SINGLETON);
    }
}

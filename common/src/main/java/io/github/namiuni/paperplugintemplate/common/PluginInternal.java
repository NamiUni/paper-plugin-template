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

import io.github.namiuni.paperplugintemplate.api.PluginTemplate;
import io.github.namiuni.paperplugintemplate.api.user.PluginTemplateUserService;
import io.github.namiuni.paperplugintemplate.common.command.CommandRegistrar;
import io.github.namiuni.paperplugintemplate.common.translation.TranslatorHolder;
import io.github.namiuni.paperplugintemplate.common.user.storage.UserRepository;
import jakarta.inject.Inject;
import net.kyori.adventure.translation.GlobalTranslator;
import org.jspecify.annotations.NullMarked;

/// Internal implementation of [PluginTemplate] that coordinates one-time
/// startup initialization across translations, storage, and the public API
/// registration.
///
/// Constructed exclusively by the Guice injector during plugin bootstrap.
/// All dependencies are supplied via constructor injection; no static accessors
/// are used within this class.
@NullMarked
public final class PluginInternal implements PluginTemplate {

    private final TranslatorHolder translatorHolder;
    private final UserRepository userRepository;
    private final PluginTemplateUserService userService;
    private final CommandRegistrar commandRegistrar;

    /// Constructs a new internal plugin facade.
    ///
    /// @param translatorHolder the holder for the active Adventure translator
    /// @param userRepository   the storage backend to initialize on [#initialize()]
    /// @param userService      the public user-service implementation to expose
    ///                         via [PluginTemplateProvider]
    @Inject
    private PluginInternal(
            final TranslatorHolder translatorHolder,
            final UserRepository userRepository,
            final PluginTemplateUserService userService,
            final CommandRegistrar commandRegistrar
    ) {
        this.translatorHolder = translatorHolder;
        this.userRepository = userRepository;
        this.userService = userService;
        this.commandRegistrar = commandRegistrar;
    }

    /// {@inheritDoc}
    @Override
    public PluginTemplateUserService userService() {
        return this.userService;
    }

    /// Performs one-time startup initialization in dependency order.
    ///
    /// The following steps are performed sequentially:
    ///
    /// 1. Registers the plugin's [net.kyori.adventure.translation.Translator]
    ///    with [GlobalTranslator] so that translatable components are resolved
    ///    for all subsequent messages.
    /// 2. Initializes the [UserRepository] (creates tables or directories).
    /// 3. Publishes this instance to [io.github.namiuni.paperplugintemplate.api.PluginTemplateProvider] so that
    ///    third-party plugins can obtain the public API reference.
    ///
    /// Must be called exactly once, during plugin bootstrap, before the
    /// server accepts player connections.
    ///
    /// @throws java.io.UncheckedIOException if the storage backend cannot be
    ///         initialized (e.g. directory creation fails for the JSON backend)
    public void initialize() {
        GlobalTranslator.translator().addSource(this.translatorHolder.get());
        this.userRepository.initialize();
        this.commandRegistrar.registerCommands();
    }
}

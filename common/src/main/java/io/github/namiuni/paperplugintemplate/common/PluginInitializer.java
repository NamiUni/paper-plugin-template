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

import io.github.namiuni.paperplugintemplate.common.command.CommandRegistrar;
import io.github.namiuni.paperplugintemplate.common.translation.TranslatorHolder;
import io.github.namiuni.paperplugintemplate.common.user.storage.UserRepository;
import jakarta.inject.Inject;
import net.kyori.adventure.translation.GlobalTranslator;
import org.jspecify.annotations.NullMarked;

/// Orchestrates one-time startup initialization across translations, storage,
/// and command registration.
///
/// This class owns the startup sequence only — it carries no public API
/// surface and is not intended to be injected outside the bootstrap path.
/// The public API contract is expressed through [io.github.namiuni.paperplugintemplate.api.PluginTemplate]
/// and provided by [PluginTemplateImpl], which is a distinct, independently
/// injectable singleton.
///
/// ## Startup sequence
///
/// [#initialize()] performs the following steps in dependency order:
///
/// 1. Registers the plugin's [net.kyori.adventure.translation.Translator]
///    with [GlobalTranslator] so translatable components resolve correctly
///    for all subsequent Adventure calls.
/// 2. Initializes the [UserRepository] (creates tables or directories).
/// 3. Registers all Cloud commands via [CommandRegistrar].
///
/// The method must be called exactly once, from the bootstrap thread, before
/// the server accepts player connections. Calling it more than once produces
/// undefined behavior (duplicate command registrations, duplicate translator
/// sources).
///
/// ## Separation of concerns
///
/// Splitting startup orchestration from API exposure serves two purposes:
///
/// - [PluginInitializer] can evolve independently of the public API; adding
///   a new startup step never touches [PluginTemplateImpl].
/// - [PluginTemplateImpl] becomes a pure, stateless value object whose sole
///   responsibility is delegating service access.
///
/// ## Thread safety
///
/// This class carries no mutable state after construction. [#initialize()]
/// itself is not thread-safe and must be called on a single thread.
@NullMarked
public final class PluginInitializer {

    private final TranslatorHolder translatorHolder;
    private final UserRepository userRepository;
    private final CommandRegistrar commandRegistrar;

    /// Constructs a new initializer with all startup dependencies.
    ///
    /// @param translatorHolder the holder for the plugin's Adventure translator;
    ///                         its value is sourced into [GlobalTranslator] during
    ///                         [#initialize()]
    /// @param userRepository   the storage backend to initialize on [#initialize()]
    /// @param commandRegistrar the registrar that publishes all Cloud commands to
    ///                         the command manager
    @Inject
    private PluginInitializer(
            final TranslatorHolder translatorHolder,
            final UserRepository userRepository,
            final CommandRegistrar commandRegistrar
    ) {
        this.translatorHolder = translatorHolder;
        this.userRepository = userRepository;
        this.commandRegistrar = commandRegistrar;
    }

    /// Performs one-time startup initialization in dependency order.
    ///
    /// The following steps execute sequentially:
    ///
    /// 1. Sources the plugin's [net.kyori.adventure.translation.Translator] into
    ///    [GlobalTranslator], making locale-aware message resolution available for
    ///    all subsequent Adventure calls.
    /// 2. Initializes [UserRepository]: creates tables (SQL backends) or
    ///    directories (JSON backend) if they do not yet exist.
    /// 3. Registers all bound [io.github.namiuni.paperplugintemplate.common.command.commands.CommandFactory]
    ///    instances with the Cloud [org.incendo.cloud.CommandManager].
    ///
    /// Must be called exactly once from the bootstrap thread before the server
    /// accepts player connections.
    ///
    /// @throws java.io.UncheckedIOException if the storage backend cannot be
    ///         initialized (e.g. directory creation fails for the JSON backend)
    public void initialize() {
        GlobalTranslator.translator().addSource(this.translatorHolder.get());
        this.userRepository.initialize();
        this.commandRegistrar.registerCommands();
    }
}

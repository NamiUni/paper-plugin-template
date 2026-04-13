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
package io.github.namiuni.paperplugintemplate.common;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.multibindings.Multibinder;
import io.github.namiuni.paperplugintemplate.api.PluginTemplate;
import io.github.namiuni.paperplugintemplate.api.user.PluginTemplateUserService;
import io.github.namiuni.paperplugintemplate.common.command.commands.CommandFactory;
import io.github.namiuni.paperplugintemplate.common.command.commands.HelpCommand;
import io.github.namiuni.paperplugintemplate.common.command.commands.ReloadCommand;
import io.github.namiuni.paperplugintemplate.common.user.UserServiceInternal;
import org.jspecify.annotations.NullMarked;

/// Guice module that binds the application-layer services shared across all
/// platform implementations.
///
/// Configures the following singleton bindings:
///
/// - [PluginTemplateUserService] → [UserServiceInternal]:
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

    private final Metadata metadata;

    /// Constructs a new `CommonModule`.
    public CommonModule(final Metadata metadata) {
        this.metadata = metadata;
    }

    @Override
    protected void configure() {
        this.bind(Metadata.class).toInstance(this.metadata);
        this.bind(PluginTemplateUserService.class).to(UserServiceInternal.class).in(Scopes.SINGLETON);
        this.bind(PluginTemplate.class).to(PluginTemplateImpl.class).in(Scopes.SINGLETON);

        this.bindCommands();
    }

    private void bindCommands() {
        final Multibinder<CommandFactory> commands = Multibinder.newSetBinder(this.binder(), CommandFactory.class);
        commands.addBinding().to(ReloadCommand.class).in(Scopes.SINGLETON);
        commands.addBinding().to(HelpCommand.class).in(Scopes.SINGLETON);
    }
}

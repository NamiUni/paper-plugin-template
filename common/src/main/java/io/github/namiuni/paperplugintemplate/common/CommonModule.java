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
import io.github.namiuni.paperplugintemplate.common.event.EventBus;
import io.github.namiuni.paperplugintemplate.common.event.SimpleEventBus;
import io.github.namiuni.paperplugintemplate.common.infrastructure.InfrastructureModule;
import io.github.namiuni.paperplugintemplate.common.user.UserServiceInternal;
import io.github.namiuni.paperplugintemplate.common.user.UserSessionHandler;
import java.nio.file.Path;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.jspecify.annotations.NullMarked;

@NullMarked
public final class CommonModule extends AbstractModule {

    private final Metadata metadata;
    private final ComponentLogger logger;
    private final Path dataDirectory;
    private final Path pluginResource;

    public CommonModule(
            final Metadata metadata,
            final ComponentLogger logger,
            final Path dataDirectory,
            final Path pluginResource
    ) {
        this.metadata = metadata;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
        this.pluginResource = pluginResource;
    }

    @Override
    protected void configure() {
        this.bind(Metadata.class).toInstance(this.metadata);
        this.bind(EventBus.class).to(SimpleEventBus.class).in(Scopes.SINGLETON);
        this.bind(PluginTemplateUserService.class).to(UserServiceInternal.class).in(Scopes.SINGLETON);
        this.bind(PluginTemplate.class).to(PluginTemplateImpl.class).in(Scopes.SINGLETON);
        this.bind(UserSessionHandler.class).asEagerSingleton();
        this.bindCommands();

        this.install(new InfrastructureModule(this.logger, this.dataDirectory, this.pluginResource));
    }

    private void bindCommands() {
        final Multibinder<CommandFactory> commands = Multibinder.newSetBinder(this.binder(), CommandFactory.class);
        commands.addBinding().to(ReloadCommand.class).in(Scopes.SINGLETON);
        commands.addBinding().to(HelpCommand.class).in(Scopes.SINGLETON);
    }
}

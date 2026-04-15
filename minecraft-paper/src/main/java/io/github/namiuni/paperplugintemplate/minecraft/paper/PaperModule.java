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
package io.github.namiuni.paperplugintemplate.minecraft.paper;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.multibindings.Multibinder;
import io.github.namiuni.paperplugintemplate.api.user.PluginTemplateUserService;
import io.github.namiuni.paperplugintemplate.common.command.CommandSource;
import io.github.namiuni.paperplugintemplate.common.user.UserFactory;
import io.github.namiuni.paperplugintemplate.minecraft.paper.command.PaperCommandSource;
import io.github.namiuni.paperplugintemplate.minecraft.paper.user.PaperUserFactory;
import io.github.namiuni.paperplugintemplate.minecraft.paper.user.UserSessionAdapter;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import jakarta.inject.Singleton;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.SenderMapper;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.paper.PaperCommandManager;
import org.jspecify.annotations.NullMarked;

@NullMarked
@SuppressWarnings({"UnstableApiUsage", "unused"})
final class PaperModule extends AbstractModule {

    private final BootstrapContext context;

    PaperModule(final BootstrapContext context) {
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
                .buildBootstrapped(this.context);
    }

    @Override
    protected void configure() {
        this.bind(JavaPlugin.class).to(PaperPlugin.class).in(Scopes.SINGLETON);
        this.bind(UserFactory.class).to(PaperUserFactory.class).in(Scopes.SINGLETON);
        this.bindAdapters();
    }

    private void bindAdapters() {
        final Multibinder<Listener> multibinder = Multibinder.newSetBinder(this.binder(), Listener.class);
        multibinder.addBinding().to(UserSessionAdapter.class).in(Scopes.SINGLETON);
    }
}

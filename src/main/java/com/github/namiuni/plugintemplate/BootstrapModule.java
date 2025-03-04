/*
 * PluginTemplate
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
package com.github.namiuni.plugintemplate;

import com.github.namiuni.plugintemplate.command.commands.PluginCommand;
import com.github.namiuni.plugintemplate.command.commands.ReloadCommand;
import com.github.namiuni.plugintemplate.translation.TranslationServiceProvider;
import com.github.namiuni.plugintemplate.translation.TranslationService;
import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.configuration.PluginMeta;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.NullMarked;

import java.nio.file.Path;

@NullMarked
@SuppressWarnings("UnstableApiUsage")
public final class BootstrapModule extends AbstractModule {

    private final BootstrapContext bootstrapContext;

    public BootstrapModule(final BootstrapContext bootstrapContext) {
        this.bootstrapContext = bootstrapContext;
    }

    @Override
    protected void configure() {
        this.bind(Path.class).annotatedWith(DataDirectory.class).toInstance(this.bootstrapContext.getDataDirectory());
        this.bind(Path.class).annotatedWith(PluginSource.class).toInstance(this.bootstrapContext.getPluginSource());
        this.bind(ComponentLogger.class).toInstance(this.bootstrapContext.getLogger());
        this.bind(PluginMeta.class).toInstance(this.bootstrapContext.getPluginMeta());
        this.bind(Key.get(new TypeLiteral<LifecycleEventManager<BootstrapContext>>() {})).toInstance(this.bootstrapContext.getLifecycleManager());

        this.bind(TranslationService.class).toProvider(TranslationServiceProvider.class).in(Scopes.SINGLETON);
        this.configureListeners();
        this.configureCommands();

        this.bind(JavaPlugin.class).to(PluginTemplate.class).in(Scopes.SINGLETON);
    }

    private void configureListeners() {
        final Multibinder<Listener> listeners = Multibinder.newSetBinder(this.binder(), Listener.class);
//        listeners.addBinding().to(MyListener.class).in(Scopes.SINGLETON);
    }

    private void configureCommands() {
        final Multibinder<PluginCommand> commands = Multibinder.newSetBinder(this.binder(), PluginCommand.class);
        commands.addBinding().to(ReloadCommand.class).in(Scopes.SINGLETON);
    }
}

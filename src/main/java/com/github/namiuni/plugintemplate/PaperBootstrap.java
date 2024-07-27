/*
 * plugin-template
 *
 * Copyright (c) 2024. Namiu (Unitarou)
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

import com.github.namiuni.plugintemplate.command.BaseCommand;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.bootstrap.PluginProviderContext;
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.plugin.java.JavaPlugin;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;

import java.util.Set;

@DefaultQualifier(NonNull.class)
@SuppressWarnings("UnstableApiUsage")
public final class PaperBootstrap implements PluginBootstrap {

    private @MonotonicNonNull Injector injector;

    @Override
    public void bootstrap(final BootstrapContext bootstrapContext) {
        this.injector = Guice.createInjector(new BootstrapModule(bootstrapContext));
        bootstrapContext.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, this::registerCommands);
    }

    @Override
    public JavaPlugin createPlugin(final PluginProviderContext context) {
        return this.injector.getInstance(PluginTemplate.class);
    }

    private void registerCommands(final ReloadableRegistrarEvent<Commands> event) {
        final var commandRegistrar = event.registrar();
        final var commands = this.injector.getInstance(Key.get(new TypeLiteral<Set<BaseCommand>>() {}));

        commands.forEach(command -> commandRegistrar.register(command.create() ,command.description(), command.aliases()));
    }
}

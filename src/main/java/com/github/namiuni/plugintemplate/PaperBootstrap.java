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

import com.github.namiuni.plugintemplate.command.CommandManager;
import com.github.namiuni.plugintemplate.configuration.ConfigurationManager;
import com.github.namiuni.plugintemplate.translation.TranslationSource;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.bootstrap.PluginProviderContext;
import org.bukkit.plugin.java.JavaPlugin;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.jspecify.annotations.NullMarked;

@NullMarked
@SuppressWarnings({"UnstableApiUsage", "unused"})
public final class PaperBootstrap implements PluginBootstrap {

    private @MonotonicNonNull Injector injector;

    @Override
    public void bootstrap(final BootstrapContext context) {
        final Module module = new BootstrapModule(context);
        this.injector = Guice.createInjector(module);

        this.injector.getInstance(ConfigurationManager.class).loadConfigurations();
        this.injector.getInstance(TranslationSource.class).loadTranslations();
        this.injector.getInstance(CommandManager.class).register();
    }

    @Override
    public JavaPlugin createPlugin(final PluginProviderContext context) {
        return this.injector.getInstance(JavaPlugin.class);
    }
}

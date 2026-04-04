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
package io.github.namiuni.paperplugintemplate.minecraft.paper;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.github.namiuni.paperplugintemplate.api.PluginTemplateProvider;
import io.github.namiuni.paperplugintemplate.common.CommonModule;
import io.github.namiuni.paperplugintemplate.common.PluginInternal;
import io.github.namiuni.paperplugintemplate.common.user.storage.StorageModule;
import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.bootstrap.PluginProviderContext;
import java.util.Objects;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/// {@inheritDoc}
@NullMarked
@SuppressWarnings({"UnstableApiUsage", "unused"})
public final class PluginBootstrapImpl implements PluginBootstrap {

    private @Nullable Injector injector;

    /// {@inheritDoc}
    @Override
    public void bootstrap(final BootstrapContext context) {
        this.injector = Guice.createInjector(
                new PluginModule(context),
                new CommonModule(),
                new StorageModule()
        );

        Objects.requireNonNull(this.injector);
        final PluginInternal plugin = this.injector.getInstance(PluginInternal.class);
        plugin.initialize();
        PluginTemplateProvider.register(plugin);
    }

    /// {@inheritDoc}
    @Override
    public JavaPlugin createPlugin(final PluginProviderContext context) {
        Objects.requireNonNull(this.injector);
        return this.injector.getInstance(JavaPlugin.class);
    }
}

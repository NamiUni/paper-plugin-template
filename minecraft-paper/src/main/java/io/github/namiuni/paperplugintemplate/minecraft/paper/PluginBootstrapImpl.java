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

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.github.namiuni.paperplugintemplate.api.PluginTemplate;
import io.github.namiuni.paperplugintemplate.api.PluginTemplateProvider;
import io.github.namiuni.paperplugintemplate.common.CommonModule;
import io.github.namiuni.paperplugintemplate.common.PluginInitializer;
import io.github.namiuni.paperplugintemplate.common.user.storage.StorageModule;
import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.bootstrap.PluginProviderContext;
import java.util.Objects;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/// Paper [PluginBootstrap] implementation that constructs the Guice injector,
/// runs the startup sequence, and publishes the public API singleton.
///
/// The bootstrap phase is split into two clearly separated responsibilities:
///
/// 1. **Initialization** — [PluginInitializer#initialize()] registers
///    translations, initializes storage, and publishes all Cloud commands.
/// 2. **API registration** — [PluginTemplateProvider#register] stores the
///    [PluginTemplate] singleton so that third-party plugins can retrieve
///    it via [PluginTemplateProvider#pluginTemplate()] from their `onEnable`.
///
/// [#createPlugin] is invoked by the Paper framework after [#bootstrap] and
/// simply retrieves the [JavaPlugin] instance from the already-built injector.
///
/// ## Thread safety
///
/// Both [#bootstrap] and [#createPlugin] are called sequentially by the Paper
/// framework on the server's main thread. The `injector` field is written once
/// in [#bootstrap] and read once in [#createPlugin]; no concurrent access
/// occurs.
@NullMarked
@SuppressWarnings({"UnstableApiUsage", "unused"})
public final class PluginBootstrapImpl implements PluginBootstrap {

    private @Nullable Injector injector;

    /// {@inheritDoc}
    ///
    /// Constructs the Guice injector, performs startup initialization via
    /// [PluginInitializer], and registers the [PluginTemplateImpl] singleton
    /// with [PluginTemplateProvider].
    @Override
    public void bootstrap(final BootstrapContext context) {
        this.injector = Guice.createInjector(
                new PluginModule(context),
                new CommonModule(context.getLogger(), context.getDataDirectory()),
                new StorageModule()
        );

        Objects.requireNonNull(this.injector);
        this.injector.getInstance(PluginInitializer.class).initialize();
        PluginTemplateProvider.register(this.injector.getInstance(PluginTemplate.class));
    }

    /// {@inheritDoc}
    @Override
    public JavaPlugin createPlugin(final PluginProviderContext context) {
        Objects.requireNonNull(this.injector);
        return this.injector.getInstance(JavaPlugin.class);
    }
}

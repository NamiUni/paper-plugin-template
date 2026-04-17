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
import io.github.namiuni.paperplugintemplate.common.CommonLifecycle;
import io.github.namiuni.paperplugintemplate.common.CommonModule;
import io.github.namiuni.paperplugintemplate.common.Metadata;
import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.bootstrap.PluginProviderContext;
import io.papermc.paper.plugin.configuration.PluginMeta;
import java.util.Objects;
import org.bukkit.plugin.java.JavaPlugin;
import org.intellij.lang.annotations.Subst;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
@SuppressWarnings({"UnstableApiUsage", "unused"})
public final class PaperBootstrap implements PluginBootstrap {

    private @Nullable Injector injector;

    @Override
    public void bootstrap(final BootstrapContext context) {
        final PluginMeta paperMeta = context.getPluginMeta();
        final @Subst("namespace") String namespace = paperMeta.namespace();
        final Metadata metadata = new Metadata(
                paperMeta.getName(),
                paperMeta.getDisplayName(),
                namespace,
                paperMeta.getVersion()
        );
        this.injector = Guice.createInjector(
                new CommonModule(metadata, context.getLogger(), context.getDataDirectory(), context.getPluginSource()),
                new PaperModule(context)
        );

        Objects.requireNonNull(this.injector);
        this.injector.getInstance(CommonLifecycle.class).bootstrap();
    }

    @Override
    public JavaPlugin createPlugin(final PluginProviderContext context) {
        Objects.requireNonNull(this.injector);
        return this.injector.getInstance(JavaPlugin.class);
    }
}

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
package io.github.namiuni.paperplugintemplate.minecraft.sponge;

import com.google.inject.Inject;
import com.google.inject.Injector;
import io.github.namiuni.paperplugintemplate.common.CommonLifecycle;
import io.github.namiuni.paperplugintemplate.common.Metadata;
import io.github.namiuni.paperplugintemplate.common.PluginBootstrapper;
import io.github.namiuni.paperplugintemplate.common.infrastructure.configuration.configurations.PrimaryConfiguration;
import io.github.namiuni.paperplugintemplate.minecraft.sponge.user.UserSessionAdapter;
import jakarta.inject.Provider;
import java.lang.invoke.MethodHandles;
import java.nio.file.Path;
import java.util.Objects;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.bstats.charts.SimplePie;
import org.bstats.sponge.Metrics;
import org.intellij.lang.annotations.Subst;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.spongepowered.api.Server;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.ConstructPluginEvent;
import org.spongepowered.api.event.lifecycle.StartedEngineEvent;
import org.spongepowered.api.event.lifecycle.StoppingEngineEvent;
import org.spongepowered.plugin.PluginContainer;
import org.spongepowered.plugin.metadata.PluginMetadata;

@NullMarked
@SuppressWarnings("unused")
public final class SpongePlugin {

    private static final int BSTATS_PLUGIN_ID = -1; // TODO

    private final PluginContainer container;
    private final ComponentLogger logger;
    private final Path configDir;
    private final Metrics.Factory metricsFactory;

    private @Nullable Injector injector;
    private @Nullable CommonLifecycle lifecycle;

    @Inject
    public SpongePlugin(
            final PluginContainer container,
            final Logger slf4jLogger,
            final @ConfigDir(sharedRoot = false) Path configDir,
            final Metrics.Factory metricsFactory
    ) {
        this.container = container;
        this.logger = ComponentLogger.logger(slf4jLogger.getName());
        this.configDir = configDir;
        this.metricsFactory = metricsFactory;
    }

    @Listener
    public void onConstruct(final ConstructPluginEvent event) {
        final PluginMetadata meta = this.container.metadata();
        final @Subst("namespace") String namespace = meta.id();
        final Metadata metadata = new Metadata(
                meta.id(),
                meta.name().orElse(meta.id()),
                namespace,
                meta.version().toString()
        );
        final Path pluginResource = Path.of(this.container.locateResource("")
                .orElseThrow(() -> new IllegalStateException("Cannot locate plugin JAR")));

        this.injector = PluginBootstrapper.bootstrap(
                metadata,
                this.logger,
                this.configDir,
                pluginResource,
                new SpongeModule(this.container)
        );
        this.lifecycle = this.injector.getInstance(CommonLifecycle.class);

        final UserSessionAdapter sessionAdapter = this.injector.getInstance(UserSessionAdapter.class);
        Sponge.eventManager().registerListeners(this.container, sessionAdapter, MethodHandles.lookup());
    }

    @Listener
    public void onStarted(final StartedEngineEvent<Server> event) {
        Objects.requireNonNull(this.lifecycle).enable();

        final Provider<PrimaryConfiguration> primaryConfig =
                Objects.requireNonNull(this.injector).getProvider(PrimaryConfiguration.class);
        this.metricsFactory.make(BSTATS_PLUGIN_ID).addCustomChart(
                new SimplePie("user_storage_type", () -> primaryConfig.get().storage().type().name())
        );
    }

    @Listener
    public void onStopping(final StoppingEngineEvent<Server> event) {
        Objects.requireNonNull(this.lifecycle).disable();
    }
}

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
import com.google.inject.Guice;
import com.google.inject.Injector;
import java.nio.file.Path;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.jspecify.annotations.NullMarked;

@NullMarked
public final class PluginBootstrapper {

    private PluginBootstrapper() {
    }

    public static Injector bootstrap(
            final Metadata metadata,
            final ComponentLogger logger,
            final Path dataDirectory,
            final Path pluginResource,
            final AbstractModule platformModule
    ) {
        final Injector injector = Guice.createInjector(
                new CommonModule(metadata, logger, dataDirectory, pluginResource),
                platformModule
        );
        injector.getInstance(CommonLifecycle.class).bootstrap();
        return injector;
    }
}

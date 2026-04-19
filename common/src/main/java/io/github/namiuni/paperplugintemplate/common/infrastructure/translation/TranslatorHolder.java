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
package io.github.namiuni.paperplugintemplate.common.infrastructure.translation;

import io.github.namiuni.paperplugintemplate.common.infrastructure.Reloadable;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import java.util.concurrent.atomic.AtomicReference;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import net.kyori.adventure.translation.Translator;
import org.jspecify.annotations.NullMarked;

@Singleton
@NullMarked
public final class TranslatorHolder implements Provider<Translator>, Reloadable<Translator> {

    private final TranslatorLoader translatorLoader;
    private final GlobalTranslatorRegistry globalRegistry;
    private final AtomicReference<Translator> translator;
    private final ComponentLogger logger;

    @Inject
    TranslatorHolder(
            final TranslatorLoader translatorLoader,
            final GlobalTranslatorRegistry globalRegistry,
            final ComponentLogger logger
    ) {
        this.translatorLoader = translatorLoader;
        this.globalRegistry = globalRegistry;
        this.logger = logger;

        this.logger.info("Loading translations...");
        final Translator initial = translatorLoader.loadTranslator();
        globalRegistry.addSource(initial);
        this.translator = new AtomicReference<>(initial);
        this.logger.info("Translations loaded.");
    }

    @Override
    public Translator reload() {
        return this.translator.updateAndGet(current -> {
            this.logger.info("Reloading translations...");
            this.globalRegistry.removeSource(current);
            final Translator fresh = this.translatorLoader.loadTranslator();
            this.globalRegistry.addSource(fresh);
            this.logger.info("Translation reload complete.");
            return fresh;
        });
    }

    @Override
    public Translator get() {
        return this.translator.get();
    }
}

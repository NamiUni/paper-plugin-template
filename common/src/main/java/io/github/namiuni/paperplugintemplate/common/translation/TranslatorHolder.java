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
package io.github.namiuni.paperplugintemplate.common.translation;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import net.kyori.adventure.translation.Translator;
import org.jspecify.annotations.NullMarked;

/// Thread-safe holder for the plugin's active Adventure [Translator].
///
/// On first construction the translator is loaded from disk via
/// [TranslatorLoader]. During a hot-reload the old translator must first be
/// removed from [net.kyori.adventure.translation.GlobalTranslator] and the
/// new instance obtained from [#reload()] must be added. The caller is
/// responsible for performing the swap atomically in terms of the
/// `GlobalTranslator` registration.
///
/// Implements [Supplier] so that consumers can obtain the current translator
/// without a direct compile-time dependency on this holder.
///
/// ## Thread safety
///
/// The active translator is stored in an [AtomicReference]. [#get()] is
/// therefore safe to call from any thread at any time. [#reload()] does
/// **not** update the stored reference; the caller is responsible for updating
/// any held references after the swap.
@Singleton
@NullMarked
public final class TranslatorHolder implements Supplier<Translator> {

    private final TranslatorLoader translatorLoader;
    private final AtomicReference<Translator> translator;
    private final ComponentLogger logger;

    /// Constructs a new holder by performing an initial translation load.
    ///
    /// @param translatorLoader the loader used for both the initial and subsequent loads
    /// @param logger           the component-aware logger
    /// @throws IOException if the translation files cannot be read during
    ///         the initial load
    @Inject
    private TranslatorHolder(
            final TranslatorLoader translatorLoader,
            final ComponentLogger logger
    ) throws IOException {
        this.translatorLoader = translatorLoader;
        this.logger = logger;

        this.logger.debug("Loading translations...");
        final Translator initial = translatorLoader.loadTranslator();
        this.translator = new AtomicReference<>(initial);
        this.logger.debug("Translations loaded.");
    }

    /// Loads a fresh [Translator] from disk and returns it.
    ///
    /// The stored reference is **not** updated by this method; callers are
    /// responsible for replacing the old source in
    /// [net.kyori.adventure.translation.GlobalTranslator] and updating any
    /// references as needed.
    ///
    /// @return a newly constructed [Translator]
    /// @throws IOException if the translation files cannot be read
    public Translator reload() throws IOException {
        this.logger.debug("Reloading translations...");
        final Translator fresh = this.translatorLoader.loadTranslator();
        this.logger.debug("Translation reload complete.");
        return fresh;
    }

    /// Returns the currently active [Translator].
    ///
    /// @return the current translator, never `null`
    @Override
    public Translator get() {
        return this.translator.get();
    }
}

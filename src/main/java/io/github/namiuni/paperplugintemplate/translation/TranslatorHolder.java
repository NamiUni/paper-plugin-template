/*
 * paper-plugin-template
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
package io.github.namiuni.paperplugintemplate.translation;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import net.kyori.adventure.translation.Translator;
import org.jspecify.annotations.NullMarked;

@Singleton
@NullMarked
public final class TranslatorHolder implements Supplier<Translator> {

    private final TranslatorLoader translatorLoader;
    private final AtomicReference<Translator> translator;

    @Inject
    private TranslatorHolder(final TranslatorLoader translatorLoader) throws IOException {
        this.translatorLoader = translatorLoader;

        final Translator initial = translatorLoader.loadTranslator();
        this.translator = new AtomicReference<>(initial);
    }

    public Translator reload() throws IOException {
        return this.translatorLoader.loadTranslator();
    }

    @Override
    public Translator get() {
        return this.translator.get();
    }
}

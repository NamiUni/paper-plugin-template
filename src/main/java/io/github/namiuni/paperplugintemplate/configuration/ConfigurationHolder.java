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
package io.github.namiuni.paperplugintemplate.configuration;

import jakarta.inject.Inject;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import org.jspecify.annotations.NullMarked;
import org.spongepowered.configurate.ConfigurateException;

@NullMarked
public final class ConfigurationHolder<T extends Record> implements Supplier<T> {

    private final ConfigurationLoader<T> configLoader;
    private final AtomicReference<T> config;

    @Inject
    private ConfigurationHolder(final ConfigurationLoader<T> configLoader) throws ConfigurateException {
        this.configLoader = configLoader;
        this.config = new AtomicReference<>(configLoader.loadConfiguration());
    }

    public T reload() throws ConfigurateException {
        final T loaded = this.configLoader.loadConfiguration();
        this.config.set(loaded);
        return loaded;
    }

    @Override
    public T get() {
        return this.config.get();
    }
}

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
package com.github.namiuni.plugintemplate.translation.placeholders;

import com.google.inject.Inject;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.moonshine.placeholder.ConclusionValue;
import net.kyori.moonshine.placeholder.ContinuanceValue;
import net.kyori.moonshine.placeholder.IPlaceholderResolver;
import net.kyori.moonshine.util.Either;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Map;

@NullMarked
public class ComponentPlaceholderResolver implements IPlaceholderResolver<Audience, Component, Component> {

    @Inject
    private ComponentPlaceholderResolver() {

    }

    @Override
    public Map<String, Either<ConclusionValue<? extends Component>, ContinuanceValue<?>>> resolve(
            final String placeholderName,
            final Component value,
            final Audience receiver,
            final Type owner,
            final Method method,
            final @Nullable Object[] parameters
    ) {
        return Map.of(placeholderName, Either.left(ConclusionValue.conclusionValue(value)));
    }
}

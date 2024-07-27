/*
 * plugin-template
 *
 * Copyright (c) 2024. Namiu (Unitarou)
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

package com.github.namiuni.plugintemplate.message;

import net.kyori.adventure.audience.Audience;
import net.kyori.moonshine.receiver.IReceiverLocator;
import net.kyori.moonshine.receiver.IReceiverLocatorResolver;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;

import java.lang.reflect.Method;
import java.lang.reflect.Type;

@DefaultQualifier(NonNull.class)
public final class AudienceReceiverResolver implements IReceiverLocatorResolver<Audience> {

    @Override
    public IReceiverLocator<Audience> resolve(final Method method, final Type proxy) {
        return new Resolver();
    }

    private static final class Resolver implements IReceiverLocator<Audience> {

        @Override
        public Audience locate(final Method method, final Object proxy, final @Nullable Object[] parameters) {

            for (final var param : parameters) {
                if (param instanceof final Audience audience) {
                    return audience;
                }
            }
            return Audience.empty();
        }
    }
}

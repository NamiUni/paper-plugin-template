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

import java.io.Serial;
import org.jspecify.annotations.NullMarked;
import org.spongepowered.configurate.ConfigurateException;

/// Unchecked wrapper for [ConfigurateException].
///
/// Use this exception to propagate Configurate failures through call sites that
/// do not or cannot declare checked exceptions (e.g. lambdas passed to functional
/// interfaces that do not throw).
@NullMarked
@SuppressWarnings("unused")
public final class UncheckedConfigurateException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = -2214743499979182898L;

    /// Constructs a new unchecked exception wrapping the given cause.
    ///
    /// @param cause the checked [ConfigurateException] to wrap
    public UncheckedConfigurateException(final ConfigurateException cause) {
        super(cause);
    }
}

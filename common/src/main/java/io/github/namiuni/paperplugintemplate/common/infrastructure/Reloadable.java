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
package io.github.namiuni.paperplugintemplate.common.infrastructure;

import org.jspecify.annotations.NullMarked;

/// Functional contract for components that support hot-reload without a server restart.
///
/// Implementations atomically replace their internal state with a freshly loaded version
/// and return the updated value so callers can chain further operations or swap
/// external registrations — for example, removing the old source from
/// [net.kyori.adventure.translation.GlobalTranslator] and adding the new one.
///
/// ## Usage pattern
///
/// ```java
/// T fresh = reloadable.reload();
/// // act on fresh value as needed
/// ```
///
/// ## Thread safety
///
/// Implementations are not required to serialize concurrent [#reload()] calls unless they
/// document otherwise.
/// [io.github.namiuni.paperplugintemplate.common.infrastructure.configuration.ConfigurationHolder]
/// and [io.github.namiuni.paperplugintemplate.common.infrastructure.translation.TranslatorHolder]
/// both use [java.util.concurrent.atomic.AtomicReference] to make their reload paths safe
/// when called from a single thread; callers must replace any held reference to the old
/// value with the returned result.
///
/// @param <T> the type of value produced by each reload
@NullMarked
@FunctionalInterface
public interface Reloadable<T> {

    /// Reloads this component and returns the refreshed value.
    ///
    /// @return the freshly loaded value, never `null`
    T reload();
}

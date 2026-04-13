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
package io.github.namiuni.paperplugintemplate.common.component;

import io.github.namiuni.paperplugintemplate.common.component.components.Component;
import org.jspecify.annotations.NullMarked;

/// Type-safe token that identifies a [Component] type within a [ComponentRegistry].
///
/// Implements the Type-Safe Heterogeneous Container key pattern (Bloch,
/// *Effective Java* 3rd ed., Item 33). Using `ComponentType<C>` instead of a
/// raw `Class<?>` preserves the component type at compile time and eliminates
/// unchecked cast warnings at call sites.
///
/// All canonical component tokens are pre-declared in [ComponentTypes].
///
/// ## Thread safety
///
/// Immutable; safe to cache and share across threads. Suitable as a
/// `ConcurrentHashMap` key — equality and hash code are delegated to the
/// wrapped `Class`.
///
/// @param <C> the component type this token identifies
@NullMarked
public record ComponentType<C extends Component>(Class<C> rawType) {
}

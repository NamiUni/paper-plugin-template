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
package io.github.namiuni.paperplugintemplate.common.component.components;

import org.jspecify.annotations.NullMarked;

/// Marker interface for all ECS component types in the plugin.
///
/// Components are pure data containers — they carry no behavioral logic.
/// Each implementation models one independent, orthogonal facet of an entity.
/// Behavior that reads from or writes to components is expressed in system
/// classes, never here.
///
/// ## Extension
///
/// Third-party plugins may introduce custom component types by implementing
/// this interface. Custom components are stored alongside built-in ones in the
/// [io.github.namiuni.paperplugintemplate.common.component.ComponentRegistry].
///
/// ## Immutability contract
///
/// Implementations **must** be effectively immutable. Mutable state inside
/// a component causes data races when multiple systems access the registry
/// concurrently. Immutable records are strongly recommended.
///
/// ## Thread safety
///
/// No state here. Thread-safety of each component is governed by its own
/// implementation.
@NullMarked
public interface Component {
}

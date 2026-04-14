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

/// Entity Component System (ECS) infrastructure for associating typed data with
/// plugin entities.
///
/// This package provides four types:
///
/// - [io.github.namiuni.paperplugintemplate.common.component.Component] — marker interface for
///   all ECS component types.
/// - [io.github.namiuni.paperplugintemplate.common.component.ComponentType] — type-safe token
///   that identifies a specific component class within the store (Bloch, *Effective Java*,
///   Item 33: Type-Safe Heterogeneous Container).
/// - [io.github.namiuni.paperplugintemplate.common.component.ComponentStore] — the central
///   thread-safe store that maps (UUID, ComponentType) pairs to component instances.
/// - [io.github.namiuni.paperplugintemplate.common.component.ComponentTypes] — pre-declared
///   token constants for all built-in component types.
///
/// ## Design philosophy
///
/// Components are pure data containers with no behavioral logic. Systems read from
/// and write to components via [io.github.namiuni.paperplugintemplate.common.component.ComponentStore].
/// This separation enables multiple orthogonal systems to share the same entity data
/// without coupling.
///
/// ## Thread safety
///
/// [io.github.namiuni.paperplugintemplate.common.component.ComponentStore] is thread-safe.
/// Component implementations must be effectively immutable per the
/// [io.github.namiuni.paperplugintemplate.common.component.components.Component] contract.
package io.github.namiuni.paperplugintemplate.common.component;

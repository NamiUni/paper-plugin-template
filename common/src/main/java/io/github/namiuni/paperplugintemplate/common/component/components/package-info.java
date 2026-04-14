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

/// Built-in ECS component interfaces for the plugin's core entity model.
///
/// This package contains the [io.github.namiuni.paperplugintemplate.common.component.components.Component]
/// marker interface and the platform-agnostic component interfaces that the `common`
/// service layer uses to access live entity data without importing platform types.
///
/// ## Built-in components
///
/// | Interface | Token | Purpose |
/// |---|---|---|
/// | [io.github.namiuni.paperplugintemplate.common.component.components.PlayerComponent] | [io.github.namiuni.paperplugintemplate.common.component.ComponentTypes#PLAYER] | Platform player handle, online status, last-seen time |
///
/// ## Adding a new component
///
/// 1. Define a new interface extending [io.github.namiuni.paperplugintemplate.common.component.components.Component]
///    in this package.
/// 2. Declare a [io.github.namiuni.paperplugintemplate.common.component.ComponentType] constant in
///    [io.github.namiuni.paperplugintemplate.common.component.ComponentTypes].
/// 3. Implement the interface in the appropriate platform adapter module.
/// 4. Register the instance in the platform [io.github.namiuni.paperplugintemplate.common.user.UserFactory].
///
/// ## Thread safety
///
/// All component implementations must be effectively immutable per the
/// [io.github.namiuni.paperplugintemplate.common.component.components.Component] contract.
package io.github.namiuni.paperplugintemplate.common.component.components;

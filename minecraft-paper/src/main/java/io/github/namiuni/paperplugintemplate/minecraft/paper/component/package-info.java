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

/// Paper platform implementations of the ECS component interfaces defined in
/// [io.github.namiuni.paperplugintemplate.common.component.components].
///
/// Each implementation in this package wraps a live Paper API object and
/// exposes only the data required by the `common` service layer, keeping all
/// Paper API imports confined to the `minecraft-paper` module.
///
/// ## Components
///
/// | Implementation | Interface | Wrapped type |
/// |---|---|---|
/// | [io.github.namiuni.paperplugintemplate.minecraft.paper.component.PaperPlayerComponent] | [io.github.namiuni.paperplugintemplate.common.component.components.PlayerComponent] | `org.bukkit.entity.Player` |
///
/// ## Adding a new Paper component
///
/// 1. Define the interface in `common/.../component/components`.
/// 2. Declare a [io.github.namiuni.paperplugintemplate.common.component.ComponentType] token
///    in [io.github.namiuni.paperplugintemplate.common.component.ComponentTypes].
/// 3. Implement the interface as a `record` in this package, wrapping the relevant
///    Paper API object.
/// 4. Register the instance in
///    [io.github.namiuni.paperplugintemplate.minecraft.paper.user.PaperUserFactory#create].
///
/// ## Thread safety
///
/// Implementations must delegate exclusively to Paper API methods that are safe to call
/// from any thread. No mutable state should be held.
package io.github.namiuni.paperplugintemplate.minecraft.paper.component;

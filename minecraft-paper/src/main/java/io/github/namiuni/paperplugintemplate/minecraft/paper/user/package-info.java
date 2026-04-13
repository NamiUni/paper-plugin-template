/*
 * PaperPluginTemplate
 *
 * Copyright (c) 2026. Namiu (うにたろう)
 *                     Contributors
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

/// Paper platform adapters that bridge live [org.bukkit.entity.Player] objects
/// to the platform-agnostic user domain.
///
/// This package contains two types:
///
/// - [io.github.namiuni.paperplugintemplate.minecraft.paper.user.PaperUser] — the Paper-specific implementation of
///   [io.github.namiuni.paperplugintemplate.common.user.UserInternal].
///   Delegates all live-data access (online status, display name, locale)
///   directly to the underlying `Player`.
/// - [io.github.namiuni.paperplugintemplate.minecraft.paper.user.PaperUserFactory] — the Paper-specific
///   [io.github.namiuni.paperplugintemplate.common.user.UserFactory] that
///   narrows the generic player type to `Player`, keeping
///   Paper API imports confined to this single class.
///
/// ## Extensibility
///
/// Adding a new player capability to the public API requires only two steps:
/// declare the method in
/// [io.github.namiuni.paperplugintemplate.api.user.PluginTemplateUser],
/// then implement it in `PaperUser` by delegating to the underlying player.
/// Neither the constructor signature nor the factory changes.
///
/// ## Thread safety
///
/// `PaperUser` profile reads and writes are lock-free and safe from any
/// thread. Live-data methods delegate to `Player`, whose
/// thread safety is governed by the Paper API contract. `PaperUserFactory`
/// is stateless and safe to bind as a Guice singleton.
package io.github.namiuni.paperplugintemplate.minecraft.paper.user;

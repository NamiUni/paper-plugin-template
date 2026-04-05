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

/// User-domain types of the public API.
///
/// This package exposes two interfaces:
///
/// - [io.github.namiuni.paperplugintemplate.api.user.PluginTemplateUser] — a live, plugin-managed view of a player that
///   combines Adventure messaging with persistent profile data.
/// - [io.github.namiuni.paperplugintemplate.api.user.PluginTemplateUserService] — the service for resolving
///   `PluginTemplateUser` instances by UUID or platform player object.
///
/// ## Choosing the right method
///
/// Use [io.github.namiuni.paperplugintemplate.api.user.PluginTemplateUserService#getUser] for non-blocking lookups when
/// the player is known to be online:
///
/// ```java
/// userService.getUser(player.getUniqueId())
///            .ifPresent(user -> user.sendMessage(component));
/// ```
///
/// Use [io.github.namiuni.paperplugintemplate.api.user.PluginTemplateUserService#loadUser] when a result must be
/// guaranteed, such as for offline-player admin commands. The returned
/// [java.util.concurrent.CompletableFuture] resolves through the repository if no cached entry exists.
///
/// ## Thread safety
///
/// Both `PluginTemplateUserService` and `PluginTemplateUser` are safe to call from any thread.
/// Compound read-modify-write sequences across multiple `PluginTemplateUser` calls are **not** atomic and require
/// external coordination when used concurrently.
package io.github.namiuni.paperplugintemplate.api.user;

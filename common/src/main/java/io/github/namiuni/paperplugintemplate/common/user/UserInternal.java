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
package io.github.namiuni.paperplugintemplate.common.user;

import io.github.namiuni.paperplugintemplate.api.user.PluginTemplateUser;
import io.github.namiuni.paperplugintemplate.common.component.components.PlayerComponent;
import org.jspecify.annotations.NullMarked;

/// Internal extension of [PluginTemplateUser] that grants the service layer and
/// platform adapters access to the ECS [PlayerComponent].
///
/// The public [PluginTemplateUser] API deliberately conceals the component system
/// from third-party consumers. `UserInternal` re-exposes the player component for
/// the two call sites within the plugin that genuinely require it:
///
/// - Platform user factories ([UserFactory] implementations) that construct and
///   register the backing [PlayerComponent] in the shared
///   [io.github.namiuni.paperplugintemplate.common.component.ComponentStore].
/// - [UserServiceInternal], which reads [PlayerComponent#isOnline()] to drive
///   the Caffeine cache expiry policy without importing a platform type.
///
/// No code outside the `common` and platform adapter modules should depend on
/// this interface; third-party integrators must use [PluginTemplateUser] exclusively.
///
/// ## Thread safety
///
/// Implementations inherit the thread-safety contract of [PluginTemplateUser]:
/// individual getter calls are safe from any thread. Compound read-modify-write
/// sequences across multiple calls are not atomic and require external coordination
/// when used concurrently.
@NullMarked
public interface UserInternal extends PluginTemplateUser {

    /// Returns the [PlayerComponent] associated with this user.
    ///
    /// Resolves the component from the shared
    /// [io.github.namiuni.paperplugintemplate.common.component.ComponentStore].
    /// In normal operation the component lifetime matches the user's cache lifetime;
    /// an absent component indicates a lifecycle-ordering error (e.g. accessing
    /// a user after its cache entry has been evicted).
    ///
    /// @return the player component, never `null`
    /// @throws io.github.namiuni.paperplugintemplate.common.component.ComponentNotFoundException
    ///         if no [PlayerComponent] is registered for this user's UUID
    PlayerComponent player();
}

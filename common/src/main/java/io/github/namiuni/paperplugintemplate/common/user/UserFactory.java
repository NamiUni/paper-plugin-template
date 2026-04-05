/*
 * PaperPluginTemplate
 *
 * Copyright (c) 2026. Namiu (ãã«ããã)
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
package io.github.namiuni.paperplugintemplate.common.user;

import io.github.namiuni.paperplugintemplate.common.user.storage.UserProfile;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.identity.Identified;
import org.jspecify.annotations.NullMarked;

/// Platform-specific factory for creating [PluginTemplateUserInternal]
/// instances.
///
/// This interface is the sole coupling point between the `common`
/// service layer and any platform adapter class. By declaring the return
/// type as [PluginTemplateUserInternal], callers can access profile
/// mutation methods without an explicit cast and without importing a
/// platform class.
///
/// Implementations reside in each platform module (e.g.
/// `minecraft-paper`) and are bound via Guice in the platform's
/// root module. The `common` module never instantiates user objects
/// directly.
///
/// ## Thread safety
///
/// Implementations are expected to be stateless and therefore safe to
/// call from any thread, including virtual threads, without additional
/// synchronization.
@NullMarked
@FunctionalInterface
public interface UserFactory {

    /// Creates a new [PluginTemplateUserInternal] bound to the given
    /// player and profile.
    ///
    /// The concrete type returned by this method is determined entirely
    /// by the platform implementation. For example, the Paper adapter
    /// returns a `PaperUser` that holds a live
    /// `org.bukkit.entity.Player` reference.
    ///
    /// @param <P>     the platform player type; must extend both
    ///                [net.kyori.adventure.audience.Audience] and
    ///                [net.kyori.adventure.identity.Identified]
    /// @param player  the live platform player; must not be `null`
    /// @param profile the initial persistent profile snapshot to
    ///                associate with `player`; must not be `null`
    /// @return a fully initialized, platform-specific user adapter, never `null`
    <P extends Audience & Identified> PluginTemplateUserInternal create(P player, UserProfile profile);
}

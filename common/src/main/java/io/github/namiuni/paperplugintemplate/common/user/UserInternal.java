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
import io.github.namiuni.paperplugintemplate.common.infrastructure.persistence.UserComponent;
import java.util.function.UnaryOperator;
import org.jspecify.annotations.NullMarked;

/// Service-layer extension of [PluginTemplateUser] that exposes profile
/// mutation for use within the `common` module.
///
/// The `common` module must not import any platform class (e.g.
/// `org.bukkit.entity.Player`). Defining the mutation contract here
/// allows [UserServiceInternal] to read and update profiles
/// through this interface alone, without casting to a concrete adapter class.
///
/// ## Implementing this interface
///
/// Platform adapters (e.g. `PaperUser`) must implement this interface
/// rather than [PluginTemplateUser] directly. [UserFactory] is declared to
/// return this type so that the service layer never needs an explicit cast.
///
/// ## Thread safety
///
/// Implementations must guarantee that [#updateProfile] is atomic and
/// immediately visible to all threads. The recommended mechanism is
/// [java.util.concurrent.atomic.AtomicReference#updateAndGet], which is
/// wait-free under no contention and lock-free under contention, and avoids
/// virtual-thread carrier-thread pinning (JEP 491).
@NullMarked
public interface UserInternal extends PluginTemplateUser {

    /// Returns the current [UserComponent] snapshot held by this user.
    ///
    /// The returned value reflects the most recently committed write from
    /// [#updateProfile]. Because [UserComponent] is an immutable record, the
    /// returned reference is safe to read from any thread without additional
    /// synchronization.
    ///
    /// @return the current profile, never `null`
    UserComponent profile();

    /// Atomically replaces the stored [UserComponent] by applying `operator`
    /// to the current value.
    ///
    /// This method must be implemented as a single atomic compare-and-swap
    /// loop (i.e. [java.util.concurrent.atomic.AtomicReference#updateAndGet])
    /// so that concurrent callers on different threads each observe a
    /// consistent before-and-after state.
    ///
    /// @param operator a pure function that accepts the current profile and
    ///                 returns the replacement; must not return `null` and
    ///                 must not produce observable side effects, as it may
    ///                 be invoked more than once under contention
    void updateProfile(UnaryOperator<UserComponent> operator);
}

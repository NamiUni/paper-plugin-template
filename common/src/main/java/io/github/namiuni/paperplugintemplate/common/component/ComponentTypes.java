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

import io.github.namiuni.paperplugintemplate.common.component.components.PlayerComponent;
import org.jspecify.annotations.NullMarked;

/// Pre-declared [ComponentType] tokens for all built-in component types.
///
/// Using shared constants instead of calling `new ComponentType<>(Foo.class)` at
/// each call site avoids repeated object allocation and guarantees referential
/// equality between tokens, which matters for the `ConcurrentHashMap` key
/// lookups inside [ComponentStore].
///
/// ## Adding new component types
///
/// Declare a new `public static final ComponentType<MyComponent>` constant here
/// when introducing a built-in component. Third-party plugins may declare their
/// own tokens in their own utility classes; they do not need to modify this class.
///
/// ## Thread safety
///
/// All constants are `static final` and therefore safely published to any thread
/// via the class-initialization guarantee.
@NullMarked
public final class ComponentTypes {

    /// Token for [PlayerComponent] — the platform player handle and its live-data accessors.
    ///
    /// Registered by the platform [io.github.namiuni.paperplugintemplate.common.user.UserFactory]
    /// implementation on every player login and removed on cache eviction.
    public static final ComponentType<PlayerComponent> PLAYER = new ComponentType<>(PlayerComponent.class);

    private ComponentTypes() {
    }
}

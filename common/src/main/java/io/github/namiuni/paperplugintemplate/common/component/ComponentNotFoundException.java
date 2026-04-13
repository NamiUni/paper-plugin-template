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

import java.io.Serial;
import java.util.UUID;
import org.jspecify.annotations.NullMarked;

/// Thrown by [ComponentRegistry#getOrThrow] and [ComponentRegistry#updateAndGet]
/// when no component of the requested type exists for the target entity.
///
/// This is a programming error — callers must either verify component
/// presence with [ComponentRegistry#has] before calling `getOrThrow`, or
/// ensure the component was registered earlier in the entity's lifecycle.
@NullMarked
public final class ComponentNotFoundException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = -5596109184340026029L;

    /// Constructs a new exception for the given uuid and missing component type.
    ///
    /// @param uuid the uuid that was queried
    /// @param type   the component type that was absent
    public ComponentNotFoundException(final UUID uuid, final ComponentType<?> type) {
        super("No component %s for uuid [%s]".formatted(type, uuid));
    }
}

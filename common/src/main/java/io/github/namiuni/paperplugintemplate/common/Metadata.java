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
package io.github.namiuni.paperplugintemplate.common;

import org.jspecify.annotations.NullMarked;

/// Immutable snapshot of plugin identity metadata sourced from the platform manifest.
///
/// Populated once during bootstrap from the platform plugin descriptor
/// (e.g. `paper-plugin.yml`) and injected wherever identity information is required
/// without importing a platform-specific type. This record is the single source of
/// truth for the plugin's name, namespace, and version across all modules.
///
/// ## Thread safety
///
/// Effectively immutable after construction; all components are `final` record
/// fields. Safe to share across threads without additional synchronization.
///
/// @param name        the internal plugin identifier (e.g. `"PaperPluginTemplate"`);
///                    used for thread-pool naming and log attribution
/// @param displayName the human-readable display name shown in the server console
/// @param namespace   the lowercase namespace used for command roots, resource keys,
///                    and connection-pool thread naming
/// @param version     the version string as declared in the plugin descriptor
@NullMarked
public record Metadata(
        String name,
        String displayName,
        String namespace,
        String version
) {
}

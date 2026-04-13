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

import io.github.namiuni.paperplugintemplate.api.PluginTemplate;
import io.github.namiuni.paperplugintemplate.api.user.PluginTemplateUserService;
import jakarta.inject.Inject;
import org.jspecify.annotations.NullMarked;

/// Singleton implementation of [PluginTemplate] that exposes the plugin's
/// public service API to third-party consumers.
///
/// This class has a single responsibility: delegating service access to the
/// injected [PluginTemplateUserService] singleton.
///
/// ## Obtaining an instance
///
/// Third-party plugins must not depend on this class directly; they should
/// use [io.github.namiuni.paperplugintemplate.api.PluginTemplateProvider#pluginTemplate()]
/// to obtain the [PluginTemplate] reference after the plugin has finished
/// bootstrapping.
///
/// ## Thread safety
///
/// This class is effectively immutable after construction: all fields are
/// `final` and the injected services are themselves thread-safe singletons.
/// [#userService()] is safe to call from any thread without additional
/// synchronization.
@NullMarked
public record PluginTemplateImpl(PluginTemplateUserService userService) implements PluginTemplate {

    @Inject
    public PluginTemplateImpl {
    }
}

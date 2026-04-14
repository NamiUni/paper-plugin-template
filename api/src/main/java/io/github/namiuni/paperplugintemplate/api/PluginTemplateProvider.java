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
package io.github.namiuni.paperplugintemplate.api;

import java.util.Objects;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class PluginTemplateProvider {

    private static @Nullable PluginTemplate instance;

    private PluginTemplateProvider() {
    }

    @ApiStatus.Internal
    public static void register(final PluginTemplate instance) {
        PluginTemplateProvider.instance = instance;
    }

    @SuppressWarnings("unused")
    public static PluginTemplate pluginTemplate() { // TODO: change the method name
        return Objects.requireNonNull(
                PluginTemplateProvider.instance,
                "%s not initialized!".formatted(PluginTemplate.class.getSimpleName())
        );
    }
}

/*
 * paper-plugin-template
 *
 * Copyright (c) 2026. Namiu/Unitarou
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

/// Paper event listeners that translate platform events into domain-service calls.
///
/// Listeners in this package must not contain business logic; they act purely as
/// adapters between the Paper event bus and the application layer. All substantive
/// behavior lives in the service classes of other packages.
@NullMarked
package io.github.namiuni.paperplugintemplate.listener;

import org.jspecify.annotations.NullMarked;

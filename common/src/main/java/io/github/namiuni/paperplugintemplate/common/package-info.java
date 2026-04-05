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

/// Cross-platform application layer shared by all platform adapters.
///
/// This package contains three types that form the structural backbone of
/// the plugin:
///
/// - [io.github.namiuni.paperplugintemplate.common.PluginInternal] — the internal implementation of
///   [io.github.namiuni.paperplugintemplate.api.PluginTemplate] that
///   sequences one-time startup: translation registration, storage
///   initialization, and command registration.
/// - [io.github.namiuni.paperplugintemplate.common.CommonModule] — the Guice module that binds application-layer
///   singletons. Installed alongside the platform-specific module during
///   injector creation.
/// - [io.github.namiuni.paperplugintemplate.common.DataDirectory] — the binding annotation that qualifies a
///   [java.nio.file.Path] injection point as the plugin's data directory.
///
/// ## Module boundary
///
/// No platform API (Paper, Sponge, Fabric, etc.) may be imported anywhere
/// in this package or its sub-packages. Platform-specific behavior is
/// expressed as interfaces here (ports) and implemented in each platform
/// module (adapters). This boundary is enforced at compile time by the
/// Gradle module structure.
///
/// ## Thread safety
///
/// `PluginInternal` and `CommonModule` carry no mutable state after the
/// Guice injector is built. All three types are safe to reference from any
/// thread once plugin initialization is complete.
package io.github.namiuni.paperplugintemplate.common;

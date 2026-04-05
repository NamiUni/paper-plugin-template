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
package io.github.namiuni.paperplugintemplate.common.command;

import net.kyori.adventure.audience.Audience;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/// Platform-agnostic representation of an Incendo Cloud command sender.
///
/// Abstracts over the underlying platform command source — e.g., Paper's
/// [io.papermc.paper.command.brigadier.CommandSourceStack] — so that all
/// command logic in the `common` module remains free of platform
/// imports. Platform adapters implement this interface and are mapped to
/// and from the native source type via a Cloud
/// [org.incendo.cloud.SenderMapper].
///
/// ## Sender vs. executor
///
/// Cloud inherits Brigadier's distinction between the *sender* (the entity
/// that issued the command) and the *executor* (the entity the command
/// physically targets when a `/execute as <entity> run …` redirect is
/// active). [#sender()] is always non-`null`; [#executor()] is `null`
/// whenever no redirect is in effect, in which case the executor is
/// considered identical to the sender.
///
/// Command handlers that need to act on a specific entity should prefer
/// [#executor()] with a `null` fallback to [#sender()]:
///
/// ```java
/// Audience target = Objects.requireNonNullElse(source.executor(), source.sender());
/// ```
///
/// ## Thread safety
///
/// Implementations must be safe to call from any thread, including Cloud's
/// async execution coordinator and virtual threads. Neither [#sender()] nor
/// [#executor()] may block the calling thread.
@NullMarked
public interface CommandSource {

    /// Returns the [Audience] representing the entity that issued the command.
    ///
    /// For player-issued commands this resolves to the plugin-managed
    /// [io.github.namiuni.paperplugintemplate.api.user.PluginTemplateUser];
    /// for console-issued commands this is the platform's console sender.
    ///
    /// @return the command sender, never `null`
    Audience sender();

    /// Returns the [Audience] representing the entity the command acts upon,
    /// or `null` when no `/execute`-style redirect is active.
    ///
    /// When non-`null`, this value differs from [#sender()] only inside an
    /// `/execute as <entity> run …` context. Command handlers that target
    /// the executor rather than the sender must null-check this value and
    /// fall back to [#sender()] as appropriate.
    ///
    /// @return the command executor, or `null` if no redirect is in effect
    @Nullable
    Audience executor();
}

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
package io.github.namiuni.paperplugintemplate.api.user;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.identity.Identified;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NullMarked;

/// A plugin-managed view of a player that combines live Adventure messaging
/// capabilities with persistent profile data stored in the configured backend.
///
/// ## Mutability contract
///
/// `lastSeen` is **read-only** on this interface; it is a system-managed
/// timestamp updated exclusively by the service layer on player disconnect
/// and must not be exposed for external mutation.
///
/// ## Thread safety
///
/// Individual getter calls are safe from any thread. The underlying profile
/// snapshot is updated via copy-on-write semantics, guaranteeing that each
/// call observes a fully constructed, consistent value without requiring
/// external synchronization.
///
/// Compound read-modify-write sequences across multiple calls are **not**
/// atomic and require external coordination when used concurrently.
///
/// ## Lifecycle
///
/// Instances are cached for the duration a player is online and for up to
/// 15 minutes after they disconnect. Obtain one via
/// [PluginTemplateUserService#getUser] for a non-blocking lookup, or
/// [PluginTemplateUserService#loadUser] to guarantee a result even on a
/// cache miss.
///
/// @see PluginTemplateUserService
@NullMarked
@ApiStatus.NonExtendable
public interface PluginTemplateUser extends Audience, Identified {

    /// Returns the player's permanent unique identifier.
    ///
    /// @return the UUID, never `null`
    UUID uuid();

    /// Returns the player's last-known username.
    ///
    /// The name reflects the value stored in the profile at the time this
    /// instance was constructed. It is updated to the current username on each
    /// new login, but does **not** change in real-time if the player changes
    /// their Minecraft username mid-session.
    ///
    /// @return the username, never `null`
    String name();

    /// Returns the player's display name as an Adventure [Component].
    ///
    /// Delegates to the underlying platform player, so the returned value
    /// reflects any custom display name set via server software.
    ///
    /// @return the display name component, never `null`
    Component displayName();

    /// Returns the locale currently active for this player.
    ///
    /// The locale is sourced from the live platform player and may differ from
    /// the server default. Used for translating
    /// [net.kyori.adventure.text.TranslatableComponent] messages.
    ///
    /// @return the player's locale, never `null`
    Locale locale();

    /// Returns the instant at which this profile was last persisted to storage.
    ///
    /// This timestamp is updated by the service layer on player disconnect and
    /// on periodic world-save checkpoints. It reflects the wall-clock time of
    /// the most recent successful write, not the current moment, and must not
    /// be modified externally.
    ///
    /// @return the last-seen timestamp, never `null`
    Instant lastSeen();

    /// Returns `true` if the underlying platform player is currently connected
    /// to the server.
    ///
    /// This method is evaluated by the cache expiry policy on every cache
    /// interaction. Online players are pinned indefinitely; offline players
    /// expire 15 minutes after their last cache access.
    ///
    /// @return `true` if the player is connected to the server
    /// @implNote On the Paper platform this method delegates to
    ///           `Player#isOnline()`, which is safe to call from any thread.
    ///           No virtual-thread pinning occurs.
    boolean isOnline();
}

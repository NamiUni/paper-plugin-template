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
package io.github.namiuni.paperplugintemplate.minecraft.paper.user;

import io.github.namiuni.paperplugintemplate.common.user.PluginTemplateUserInternal;
import io.github.namiuni.paperplugintemplate.common.user.storage.UserProfile;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.ForwardingAudience;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/// Paper-specific [PluginTemplateUserInternal] adapter that wraps a live
/// [Player].
///
/// All live-data access — online status, display name, locale, and any
/// future additions such as health or inventory — delegates directly to
/// the underlying [Player], eliminating the supplier-based workarounds
/// that arise when a platform-agnostic class tries to abstract over
/// platform behavior.
///
/// ## Extensibility
///
/// Adding a new capability to the public API requires only two steps:
///
/// 1. Declare the method in [io.github.namiuni.paperplugintemplate.api.user.PluginTemplateUser] (the port).
/// 2. Implement it here by delegating to `this.player`.
///
/// The constructor signature never changes as capabilities are added.
///
/// ## Thread safety
///
/// The persistent [UserProfile] is held in an [java.util.concurrent.atomic.AtomicReference],
/// enabling lock-free copy-on-write updates from any thread. No
/// `synchronized` blocks are used; this class is therefore free from
/// virtual-thread carrier-thread pinning (JEP 491).
///
/// Live-data methods such as [#isOnline()] and [#displayName()] delegate
/// to the underlying [Player], whose thread-safety is governed by the
/// Paper API contract.
@NullMarked
public final class PaperUser implements PluginTemplateUserInternal, ForwardingAudience.Single {

    private final Player player;
    private final AtomicReference<UserProfile> profile;

    /// Constructs a new adapter binding a live player to an initial
    /// profile snapshot.
    ///
    /// @param player  the live Paper player used for all platform
    ///                delegation; must not be `null`
    /// @param profile the initial persistent profile snapshot; must
    ///                not be `null`
    public PaperUser(final Player player, final UserProfile profile) {
        this.player = player;
        this.profile = new AtomicReference<>(profile);
    }

    /// {@inheritDoc}
    ///
    /// The returned snapshot is always fully constructed and safe to read
    /// from any thread; [UserProfile] is an immutable record.
    @Override
    public UserProfile profile() {
        return this.profile.get();
    }

    /// {@inheritDoc}
    ///
    /// @implNote Delegates to [java.util.concurrent.atomic.AtomicReference#updateAndGet],
    ///           which is wait-free under no contention and lock-free
    ///           under contention. Because no `synchronized` block is
    ///           used, calling this method from a virtual thread never
    ///           pins the carrier thread.
    @Override
    public void updateProfile(final UnaryOperator<UserProfile> operator) {
        this.profile.updateAndGet(operator);
    }

    /// Returns `true` if the underlying player is currently connected
    /// to the server.
    ///
    /// Delegates directly to [Player#isOnline()], which is safe to call
    /// from any thread per the Paper API contract.
    ///
    /// @return `true` if the player is connected to the server
    @Override
    public boolean isOnline() {
        return this.player.isOnline();
    }

    /// {@inheritDoc}
    @Override
    public UUID uuid() {
        return this.player.getUniqueId();
    }

    /// {@inheritDoc}
    @Override
    public String name() {
        return this.player.getName();
    }

    /// {@inheritDoc}
    @Override
    public Component displayName() {
        return this.player.displayName();
    }

    /// {@inheritDoc}
    @Override
    public Locale locale() {
        return this.player.locale();
    }

    /// {@inheritDoc}
    @Override
    public Instant lastSeen() {
        return Instant.ofEpochMilli(this.player.getLastSeen());
    }

    /// {@inheritDoc}
    @Override
    public Audience audience() {
        return this.player;
    }

    /// {@inheritDoc}
    @Override
    public Identity identity() {
        return this.player.identity();
    }

    @Override
    public String toString() {
        return "PaperUser{" +
                "player=" + this.player +
                ", profile=" + this.profile +
                '}';
    }

    @Override
    public boolean equals(final @Nullable Object that) {
        if (that == null || getClass() != that.getClass()) {
            return false;
        }

        final PaperUser paperUser = (PaperUser) that;
        return Objects.equals(this.player, paperUser.player) && Objects.equals(this.profile.get(), paperUser.profile.get());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.player, this.profile);
    }
}

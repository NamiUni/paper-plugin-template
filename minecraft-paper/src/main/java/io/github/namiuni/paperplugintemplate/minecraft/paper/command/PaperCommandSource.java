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
package io.github.namiuni.paperplugintemplate.minecraft.paper.command;

import io.github.namiuni.paperplugintemplate.api.user.PluginTemplateUserService;
import io.github.namiuni.paperplugintemplate.common.command.CommandSource;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.audience.Audience;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/// Paper-specific [CommandSource] adapter that bridges Paper's
/// [CommandSourceStack] to the platform-agnostic command API.
///
/// Resolves the sender and executor from the underlying
/// [CommandSourceStack]: when the sender or executor is a
/// [org.bukkit.entity.Player], the corresponding plugin-managed
/// [io.github.namiuni.paperplugintemplate.api.user.PluginTemplateUser] is
/// returned via [io.github.namiuni.paperplugintemplate.api.user.PluginTemplateUserService#getUser];
/// non-player senders (e.g., the console) are passed through as-is.
///
/// This class is also used as the reverse-mapping target in
/// [org.incendo.cloud.SenderMapper]: [#paperSource()] exposes the
/// underlying [CommandSourceStack] so Cloud can reconstruct the native
/// source when required by the Brigadier integration.
///
/// ## Invariant
///
/// [#sender()] and [#executor()] both call `getUser(uuid).orElseThrow()`.
/// This is safe only when invoked after
/// [io.github.namiuni.paperplugintemplate.common.user.UserServiceInternal#loadUser]
/// has completed for the player — which is guaranteed for any command
/// handler running after the `PlayerJoinEvent`. Invoking either method
/// during the configuration phase (before join) for a player whose userCache
/// entry has not yet been populated will result in an exception.
///
/// ## Thread safety
///
/// This class is effectively immutable after construction: both fields are
/// `final` and neither [#sender()] nor [#executor()] mutates state. Safe to
/// call from Cloud's async execution coordinator (virtual threads) without
/// additional synchronization. The [PluginTemplateUserService#getUser] call
/// is non-blocking.
@NullMarked
public final class PaperCommandSource implements CommandSource {

    private final CommandSourceStack source;
    private final PluginTemplateUserService userService;

    /// Constructs a new adapter wrapping the given Paper command source.
    ///
    /// @param source      the Paper Brigadier command source stack; must not
    ///                    be `null`
    /// @param userService the user service used to resolve online players to
    ///                    their [PluginTemplateUser]
    ///                    counterparts; must not be `null`
    public PaperCommandSource(
            final CommandSourceStack source,
            final PluginTemplateUserService userService
    ) {
        this.source = source;
        this.userService = userService;
    }

    /// Returns the underlying [CommandSourceStack] for use in the Cloud
    /// [org.incendo.cloud.SenderMapper] reverse-mapping path.
    ///
    /// This method exists solely to satisfy the mapper's
    /// {@code reverseMapper} lambda; it should not be called from command
    /// handler logic.
    ///
    /// @return the wrapped Paper command source stack, never `null`
    public CommandSourceStack paperSource() {
        return this.source;
    }

    /// {@inheritDoc}
    ///
    /// When the underlying sender is a [Player], returns
    /// the corresponding [PluginTemplateUser]
    /// from the user service userCache. Otherwise, returns the raw
    /// [org.bukkit.command.CommandSender] (e.g., the console).
    @Override
    public Audience sender() {
        if (this.source.getSender() instanceof final Player player) {
            return this.userService.getUser(player.getUniqueId()).orElseThrow();
        }
        return this.source.getSender();
    }

    /// {@inheritDoc}
    ///
    /// When the underlying executor is a [Player], returns
    /// the corresponding [PluginTemplateUser]
    /// from the user service userCache. Returns `null` when
    /// [CommandSourceStack#getExecutor()] is `null` — i.e., when no
    /// `/execute as` redirect is active.
    @Override
    public @Nullable Audience executor() {
        if (this.source.getExecutor() instanceof final Player player) {
            return this.userService.getUser(player.getUniqueId()).orElseThrow();
        }
        return this.source.getExecutor();
    }
}

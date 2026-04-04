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
package io.github.namiuni.paperplugintemplate.listener;

import io.github.namiuni.paperplugintemplate.common.translation.Messages;
import io.github.namiuni.paperplugintemplate.common.user.PluginTemplateUserServiceInternal;
import io.papermc.paper.event.connection.configuration.AsyncPlayerConnectionConfigureEvent;
import jakarta.inject.Inject;
import java.util.UUID;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.WorldSaveEvent;
import org.jspecify.annotations.NullMarked;

/// Bukkit event listener that bridges Paper lifecycle events to the user
/// service.
///
/// Handles four lifecycle points for profile management:
///
/// - **Pre-connect** ([AsyncPlayerConnectionConfigureEvent]): pre-loads
///   the player's profile into the pre-load cache so that the first
///   gameplay access is non-blocking. If the repository is unreachable,
///   the player is disconnected with a localized error message rather than
///   joining without a valid profile.
///
/// - **Join** ([PlayerJoinEvent]): promotes the preloaded profile from the
///   connection cache into the user cache, ensuring all subsequent service
///   calls are guaranteed cache hits for the duration of the session.
///
/// - **Disconnect** ([PlayerQuitEvent]): stamps `lastSeen`, persists the
///   final profile snapshot, and evicts the cache entry. Cache eviction is
///   guaranteed by the service's persist operation regardless of whether
///   the repository write succeeds; this handler additionally discards the
///   cache entry manually only when the initial load future itself failed.
///
/// - **World save** ([WorldSaveEvent]): checkpoints all profiles for
///   players in the saved world to limit data loss on unexpected shutdowns.
///
/// ## Thread safety
///
/// The callback for [AsyncPlayerConnectionConfigureEvent] fires on Paper's
/// async configuration thread. All other callbacks fire on the main server
/// thread. The user service methods called here are safe from all threads;
/// no additional synchronization is required.
@NullMarked
@SuppressWarnings({"UnstableApiUsage", "unused"})
public final class PaperEventHandler implements Listener {

    private final PluginTemplateUserServiceInternal userService;
    private final ComponentLogger logger;
    private final Messages messages;

    /// Constructs a new event handler.
    ///
    /// @param userService the internal service managing in-memory and
    ///                    persistent user state
    /// @param logger      the component-aware logger used for structured
    ///                    error reporting
    /// @param messages    the localized message provider
    @Inject
    private PaperEventHandler(
            final PluginTemplateUserServiceInternal userService,
            final ComponentLogger logger,
            final Messages messages) {
        this.userService = userService;
        this.logger = logger;
        this.messages = messages;
    }

    /// Pre-loads the connecting player's profile into the service cache.
    ///
    /// Fires on Paper's async configuration thread before a `Player` object
    /// exists. A load failure disconnects the player with a localized error
    /// message so they never join without a valid profile.
    ///
    /// @param event the async connection configure event
    @EventHandler(priority = EventPriority.MONITOR)
    private void onConnect(final AsyncPlayerConnectionConfigureEvent event) {
        final var connection = event.getConnection();
        if (!connection.isConnected()) {
            return;
        }

        final UUID uuid = connection.getProfile().getId();
        if (uuid == null) {
            return;
        }

        this.userService.loadUserProfile(uuid)
                .whenComplete((profile, ex) -> {
                    if (ex != null) {
                        this.logger.error("Failed to pre-load profile for UUID: {}; disconnecting.", uuid, ex);
                        connection.disconnect(this.messages.joinFailureProfile());
                    } else {
                        this.logger.debug("Pre-load succeeded for UUID: {}, result: {}", uuid, profile);
                    }
                });
    }

    /// Promotes the preloaded profile into the user cache on join.
    ///
    /// Ensures all subsequent service calls during this session are
    /// guaranteed cache hits.
    ///
    /// @param event the join event
    @EventHandler(priority = EventPriority.MONITOR)
    private void onJoin(final PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        this.userService.loadUser(player)
                .whenComplete((user, ex) -> {
                    if (ex != null) {
                        this.logger.error("Failed to load user on join for UUID: {}", player.getUniqueId(), ex);
                    } else {
                        this.logger.debug("Join cache promotion succeeded: {}", user);
                    }
                });
    }

    /// Persists the final profile snapshot and evicts the cache entry on
    /// disconnect.
    ///
    /// The service's persist operation stamps `lastSeen` with the current
    /// instant and guarantees cache eviction via its own completion
    /// handler, regardless of whether the repository write succeeds or
    /// fails. This handler therefore only discards the cache entry manually
    /// as a safety net for the case where the initial load future itself
    /// fails — in which case the persist call is never reached and the
    /// entry would otherwise leak until it expires naturally.
    ///
    /// @param event the quit event
    @EventHandler(priority = EventPriority.MONITOR)
    private void onDisconnect(final PlayerQuitEvent event) {
        final Player player = event.getPlayer();
        final UUID uuid = player.getUniqueId();
        this.userService.loadUser(player)
                .thenCompose(user -> {
                    this.logger.debug("Disconnecting: {}", user);
                    return this.userService.persistOnlinePlayer(user);
                    // persistOnlinePlayer guarantees discardUser on completion;
                    // no additional eviction call is needed on the success path.
                })
                .whenComplete((_, ex) -> {
                    if (ex != null) {
                        this.logger.error("Failed to persist profile on disconnect for UUID: {}", uuid, ex);
                        // Safety net: persistOnlinePlayer was never reached, so the
                        // cache entry must be evicted here to avoid a leak.
                        this.userService.discardUser(uuid);
                    }
                });
    }

    /// Checkpoints profiles for all online players in the saved world.
    ///
    /// Reduces data loss exposure on unexpected server shutdowns. Each
    /// checkpoint stamps `lastSeen` and persists the current profile
    /// state. Failures are logged per-player and do not interrupt the
    /// world-save process.
    ///
    /// @param event the world save event
    @EventHandler(priority = EventPriority.MONITOR)
    private void onWorldSave(final WorldSaveEvent event) {
        for (final Player player : event.getWorld().getPlayers()) {
            this.userService.loadUser(player)
                    .thenCompose(user -> {
                        this.logger.debug("World-save checkpoint: {}", user);
                        return this.userService.persistOnlinePlayer(user);
                    })
                    .whenComplete((_, ex) -> {
                        if (ex != null) {
                            this.logger.error("Failed to persist profile on world save for UUID: {}", player.getUniqueId(), ex);
                        }
                    });
        }
    }
}

package io.github.namiuni.paperplugintemplate.api.user;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.BooleanSupplier;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.identity.Identified;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NullMarked;

/// Service for accessing and managing [PluginTemplateUser] instances.
///
/// Callers on the Paper platform should always pass `player::isOnline` as the
/// `onlineCheck` argument to [#loadUser]. This supplier is evaluated by the
/// cache expiry policy on every cache interaction to determine whether the entry
/// should be pinned indefinitely (online) or allowed to expire after 15 minutes
/// of inactivity (offline).
///
/// Passing `() -> false` is correct for offline-player lookups (e.g. admin commands)
/// where no live connection exists.
@NullMarked
@ApiStatus.NonExtendable
public interface PluginTemplateUserService {

    /// Returns the cached [PluginTemplateUser] for `player`, if present.
    ///
    /// This method never blocks and never triggers a repository lookup.
    /// Returns empty if the player has not been loaded or has already been evicted.
    ///
    /// @param <P>    the platform player type
    /// @param player the player to look up
    /// @return the cached user, or empty on a cache miss
    <P extends Audience & Identified> Optional<PluginTemplateUser> getUser(P player);

    /// Returns the [PluginTemplateUser] for `player`, loading from the repository
    /// if necessary.
    ///
    /// Resolution order:
    ///
    /// 1. In-memory user cache (non-blocking).
    /// 2. Connection pre-load cache (non-blocking; populated during
    ///    [io.papermc.paper.event.connection.configuration.AsyncPlayerConnectionConfigureEvent]).
    /// 3. Repository (async I/O).
    ///
    /// The resolved entry is cached with an expiry determined by `onlineCheck`:
    /// `true` pins the entry indefinitely; `false` allows it to expire after
    /// 15 minutes of inactivity.
    ///
    /// @param <P>         the platform player type
    /// @param player      the player to load
    /// @param onlineCheck a supplier evaluated on every cache interaction to determine
    ///                    online status; use `player::isOnline` on Paper,
    ///                    `() -> false` for offline lookups
    /// @return a future resolving to the user; never completes with `null`
    <P extends Audience & Identified> CompletableFuture<PluginTemplateUser> loadUser(P player, BooleanSupplier onlineCheck);

    /// Removes all persisted data for `uuid`.
    ///
    /// @param uuid the player UUID to delete
    /// @return a future that completes when the deletion finishes
    CompletableFuture<Void> deleteUser(UUID uuid);
}

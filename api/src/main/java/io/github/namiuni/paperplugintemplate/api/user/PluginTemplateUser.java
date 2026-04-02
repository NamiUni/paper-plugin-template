package io.github.namiuni.paperplugintemplate.api.user;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.identity.Identified;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NullMarked;

/// A plugin-managed view of a player combining live Adventure capabilities
/// with persistent profile data.
///
/// The interface is intentionally mutable for user-configurable fields, following
/// the same model as Bukkit's [org.bukkit.entity.Player]. This allows command
/// handlers to read and write player state in a natural, imperative style without
/// constructing intermediate value objects.
///
/// ## Mutability contract
///
/// Setters for user-configurable fields are added as the template is extended.
/// `lastSeen` is **read-only** on this interface; it is a system-managed timestamp
/// updated exclusively by the service layer and must not be exposed to external
/// mutation.
///
/// ## Thread safety
///
/// Individual setter calls are atomic (backed by [java.util.concurrent.atomic.AtomicReference]
/// in the implementation), but compound read-modify-write sequences are not.
///
/// ## Lifecycle
///
/// Instances are cached for the duration a player is online (and up to 15 minutes
/// after going offline). Obtain one via [PluginTemplateUserService#getUser] for
/// a non-blocking lookup, or [PluginTemplateUserService#loadUser] to guarantee
/// a result even on a cache miss.
@NullMarked
@ApiStatus.NonExtendable
public interface PluginTemplateUser extends Audience, Identified {

    /// Returns the player's permanent unique identifier.
    ///
    /// @return the UUID, never `null`
    UUID uuid();

    /// Returns the player's current username.
    ///
    /// @return the username, never `null`
    String name();

    /// Returns the player's display name as an Adventure [Component].
    ///
    /// @return the display name, never `null`
    Component displayName();

    /// Returns the player's active locale.
    ///
    /// @return the locale, never `null`
    Locale locale();

    /// Returns the instant at which this profile was last persisted to storage.
    ///
    /// @return the last-seen timestamp, never `null`
    Instant lastSeen();

    /// Returns `true` if the underlying platform player is currently connected
    /// to the server.
    ///
    /// This method is evaluated by the cache's [com.github.benmanes.caffeine.cache.Expiry]
    /// policy on every cache interaction. Online players are pinned in cache
    /// indefinitely; offline players expire 15 minutes after their last access.
    ///
    /// @return `true` if the player is online
    boolean isOnline();
}

package io.github.namiuni.paperplugintemplate.common.user;

import io.github.namiuni.paperplugintemplate.common.user.storage.UserProfile;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.identity.Identified;
import org.jspecify.annotations.NullMarked;

/// Platform-specific factory for creating [PluginTemplateUserInternal]
/// instances.
///
/// This interface is the sole coupling point between the `common`
/// service layer and any platform adapter class. By declaring the return
/// type as [PluginTemplateUserInternal], callers can access profile
/// mutation methods without an explicit cast and without importing a
/// platform class.
///
/// Implementations reside in each platform module (e.g.
/// `minecraft-paper`) and are bound via Guice in the platform's
/// root module. The `common` module never instantiates user objects
/// directly.
///
/// ## Thread safety
///
/// Implementations are expected to be stateless and therefore safe to
/// call from any thread, including virtual threads, without additional
/// synchronization.
@NullMarked
@FunctionalInterface
public interface UserFactory {

    /// Creates a new [PluginTemplateUserInternal] bound to the given
    /// player and profile.
    ///
    /// The concrete type returned by this method is determined entirely
    /// by the platform implementation. For example, the Paper adapter
    /// returns a `PaperUser` that holds a live
    /// `org.bukkit.entity.Player` reference.
    ///
    /// @param <P>     the platform player type; must extend both
    ///                [net.kyori.adventure.audience.Audience] and
    ///                [net.kyori.adventure.identity.Identified]
    /// @param player  the live platform player; must not be `null`
    /// @param profile the initial persistent profile snapshot to
    ///                associate with `player`; must not be `null`
    /// @return a fully initialized, platform-specific user adapter, never `null`
    <P extends Audience & Identified> PluginTemplateUserInternal create(P player, UserProfile profile);
}

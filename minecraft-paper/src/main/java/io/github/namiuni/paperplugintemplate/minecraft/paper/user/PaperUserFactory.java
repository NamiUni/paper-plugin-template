package io.github.namiuni.paperplugintemplate.minecraft.paper.user;

import io.github.namiuni.paperplugintemplate.common.user.PluginTemplateUserInternal;
import io.github.namiuni.paperplugintemplate.common.user.UserFactory;
import io.github.namiuni.paperplugintemplate.common.user.storage.UserProfile;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.identity.Identified;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

/// [UserFactory] implementation for the Paper platform.
///
/// Performs the platform-specific narrowing cast from the generic
/// `<P extends Audience & Identified>` type parameter to
/// [org.bukkit.entity.Player], confining that knowledge to this single
/// class so that neither [io.github.namiuni.paperplugintemplate.common.user.PluginTemplateUserServiceInternal]
/// nor any other {@code common}-module class imports a Paper API type.
///
/// This class carries no mutable state and is safe to bind as a Guice
/// singleton.
///
/// ## Thread safety
///
/// This class is stateless and therefore safe to call from any thread,
/// including virtual threads, without additional synchronization.
@NullMarked
public final class PaperUserFactory implements UserFactory {

    /// Constructs a new `PaperUserFactory`.
    public PaperUserFactory() {
    }

    /// Creates a new [PaperUser] by narrowing `player` to
    /// [org.bukkit.entity.Player] and pairing it with `profile`.
    ///
    /// The cast is safe within the Paper platform because every player
    /// object passed by the Paper event system satisfies
    /// `player instanceof Player`. Invoking this factory outside the
    /// Paper platform — for example by binding it in a Sponge module —
    /// is a programming error and will fail immediately at this cast.
    ///
    /// @param <P>     the platform player type; must be [org.bukkit.entity.Player] at runtime
    /// @param player  the connecting player; must not be `null` and must be an instance of [org.bukkit.entity.Player]
    /// @param profile the initial persistent profile snapshot to associate with `player`; must not be `null`
    /// @return a new [PaperUser] bound to `player` and `profile`, never `null`
    /// @throws ClassCastException if `player` is not an instance of [org.bukkit.entity.Player];
    ///                            this indicates a misconfigured Guice binding for a non-Paper platform
    @Override
    public <P extends Audience & Identified> PluginTemplateUserInternal create(final P player, final UserProfile profile) {
        return new PaperUser((Player) player, profile);
    }
}

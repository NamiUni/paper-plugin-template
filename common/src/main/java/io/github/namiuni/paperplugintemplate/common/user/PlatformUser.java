package io.github.namiuni.paperplugintemplate.common.user;

import io.github.namiuni.paperplugintemplate.api.user.PluginTemplateUser;
import io.github.namiuni.paperplugintemplate.common.user.storage.UserProfile;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.UnaryOperator;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.ForwardingAudience;
import net.kyori.adventure.identity.Identified;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/// Platform-specific [PluginTemplateUser] implementation that pairs a live player
/// object with its persisted [UserProfile].
///
/// The underlying [UserProfile] is held in an [AtomicReference] to allow
/// individual field setters to perform atomic copy-on-write updates safely from
/// any thread, mirroring the concurrency model of Bukkit's [org.bukkit.entity.Player].
///
/// Online status is determined by a [BooleanSupplier] injected at construction
/// time. On the Paper platform this supplier delegates to `Player#isOnline()`.
/// The supplier is intentionally platform-agnostic so that `PlatformUser` itself
/// carries no dependency on the Paper API.
///
/// ## Mutation model
///
/// Every setter replaces the stored [UserProfile] with a new instance derived via
/// a `withX()` accessor method. Changes are visible immediately within the same
/// [PlatformUser] instance and propagated to storage on the next
/// [PluginTemplateUserServiceInternal#upsertUser] call.
///
/// @param <P> the platform player type; must extend both [Audience] and [Identified]
@NullMarked
public final class PlatformUser<P extends Audience & Identified> implements PluginTemplateUser, ForwardingAudience.Single {

    private final Audience player;
    private final AtomicReference<UserProfile> profile;
    private final BooleanSupplier onlineCheck;

    /// Constructs a new [PlatformUser].
    ///
    /// @param player      the live platform player used for audience delegation and
    ///                    live identity pointers such as display name and locale
    /// @param profile     the persisted profile snapshot to associate with this player
    /// @param onlineCheck a supplier returning `true` while the player is connected;
    ///                    use `player::isOnline` on Paper, `() -> false` for offline
    ///                    player representations
    public PlatformUser(final P player, final UserProfile profile, final BooleanSupplier onlineCheck) {
        this.player = player;
        this.profile = new AtomicReference<>(profile);
        this.onlineCheck = onlineCheck;
    }

    /// Returns the current [UserProfile] snapshot.
    ///
    /// Package-private; consumers outside the `user` package interact through
    /// the [PluginTemplateUser] interface only.
    ///
    /// @return the current profile, never `null`
    UserProfile profile() {
        return this.profile.get();
    }

    /// Atomically replaces the stored [UserProfile] using the given operator.
    ///
    /// Package-private; called by [PluginTemplateUserServiceInternal] to update
    /// system-managed fields (e.g. `lastSeen`) without exposing those mutations
    /// on the public interface.
    ///
    /// @param operator the pure function producing the updated profile
    void updateProfile(final UnaryOperator<UserProfile> operator) {
        this.profile.updateAndGet(operator);
    }

    @Override
    public boolean isOnline() {
        return this.onlineCheck.getAsBoolean();
    }

    @Override
    public UUID uuid() {
        return this.profile.get().uuid();
    }

    @Override
    public String name() {
        return this.profile.get().name();
    }

    @Override
    public Component displayName() {
        return this.get(Identity.DISPLAY_NAME).orElseThrow();
    }

    @Override
    public Locale locale() {
        return this.get(Identity.LOCALE).orElseThrow();
    }

    @Override
    public Instant lastSeen() {
        return this.profile.get().lastSeen();
    }

    @Override
    public Identity identity() {
        return Identity.identity(this.profile.get().uuid());
    }

    @Override
    public Audience audience() {
        return this.player;
    }

    @Override
    public String toString() {
        return "PlatformUser{" +
                "player=" + this.player +
                ", profile=" + this.profile +
                ", onlineCheck=" + this.onlineCheck +
                '}';
    }

    @Override
    public boolean equals(final @Nullable Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        final PlatformUser<?> that = (PlatformUser<?>) o;
        return Objects.equals(this.player, that.player) && Objects.equals(this.profile.get(), that.profile.get()) && Objects.equals(this.onlineCheck, that.onlineCheck);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.player, this.profile, this.onlineCheck);
    }
}

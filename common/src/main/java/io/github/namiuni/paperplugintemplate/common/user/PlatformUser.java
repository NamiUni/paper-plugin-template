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

/// Platform-specific [PluginTemplateUser] implementation that pairs a live
/// player object with its persisted [UserProfile].
///
/// ## Thread safety
///
/// The underlying [UserProfile] is held in an [AtomicReference], enabling
/// individual setters to perform lock-free copy-on-write updates safely from
/// any thread — including virtual threads. No `synchronized` blocks are used;
/// this class is therefore free from virtual-thread carrier-thread pinning
/// (JEP 491).
///
/// Compound read-modify-write sequences across multiple setter calls are
/// **not** atomic; callers requiring such atomicity must coordinate
/// externally.
///
/// ## Online-status delegation
///
/// Online status is determined by the [BooleanSupplier] injected at
/// construction time rather than by this class directly. On the Paper
/// platform the supplier delegates to `Player#isOnline()`, keeping this
/// class free of any Paper API dependency.
///
/// ## Mutation model
///
/// Every setter derives a fresh [UserProfile] via a `withX()` method and
/// atomically installs it with [AtomicReference#updateAndGet]. The change is
/// immediately visible within this instance and is propagated to storage on
/// the next persistence call.
///
/// @param <P> the platform player type; must extend both [Audience] and
///            [Identified]
@NullMarked
public final class PlatformUser<P extends Audience & Identified>
        implements PluginTemplateUser, ForwardingAudience.Single {

    private final Audience player;
    private final AtomicReference<UserProfile> profile;
    private final BooleanSupplier onlineCheck;

    /// Constructs a new `PlatformUser` binding a live player to a profile
    /// snapshot.
    ///
    /// @param player      the live platform player used for audience
    ///                    delegation and identity pointers such as display
    ///                    name and locale
    /// @param profile     the persisted profile snapshot to associate with
    ///                    this player
    /// @param onlineCheck a supplier returning `true` while the player is
    ///                    connected; pass `player::isOnline` on Paper, or
    ///                    `() -> false` for offline player representations
    public PlatformUser(
            final P player,
            final UserProfile profile,
            final BooleanSupplier onlineCheck) {
        this.player = player;
        this.profile = new AtomicReference<>(profile);
        this.onlineCheck = onlineCheck;
    }

    /// Returns the current [UserProfile] snapshot.
    ///
    /// Package-private; consumers outside the `user` package interact only
    /// through the [PluginTemplateUser] interface.
    ///
    /// @return the current profile, never `null`
    UserProfile profile() {
        return this.profile.get();
    }

    /// Atomically replaces the stored [UserProfile] by applying `operator`
    /// to the current value.
    ///
    /// Package-private; called by the user service to update system-managed
    /// fields such as `lastSeen` without exposing those mutations on the
    /// public interface.
    ///
    /// @param operator a pure function producing the updated profile; must
    ///                 not return `null` and must not have side effects
    ///                 beyond constructing the new record
    /// @implNote Delegates to [AtomicReference#updateAndGet], which is
    ///           wait-free under no contention and lock-free under contention.
    ///           Safe to call from virtual threads without carrier-thread
    ///           pinning.
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
        return "PlatformUser{"
                + "player=" + this.player
                + ", profile=" + this.profile
                + ", onlineCheck=" + this.onlineCheck
                + '}';
    }

    @Override
    public boolean equals(final @Nullable Object o) {
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        final PlatformUser<?> that = (PlatformUser<?>) o;
        return Objects.equals(this.player, that.player)
                && Objects.equals(this.profile.get(), that.profile.get())
                && Objects.equals(this.onlineCheck, that.onlineCheck);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.player, this.profile, this.onlineCheck);
    }
}

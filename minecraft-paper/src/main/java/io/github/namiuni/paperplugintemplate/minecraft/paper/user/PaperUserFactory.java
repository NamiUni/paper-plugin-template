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
package io.github.namiuni.paperplugintemplate.minecraft.paper.user;

import io.github.namiuni.paperplugintemplate.common.component.ComponentStore;
import io.github.namiuni.paperplugintemplate.common.component.ComponentTypes;
import io.github.namiuni.paperplugintemplate.common.infrastructure.persistence.UserRecord;
import io.github.namiuni.paperplugintemplate.common.user.UserFactory;
import io.github.namiuni.paperplugintemplate.common.user.UserInternal;
import io.github.namiuni.paperplugintemplate.minecraft.paper.component.PaperPlayerComponent;
import jakarta.inject.Inject;
import java.util.UUID;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.identity.Identified;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

/// [UserFactory] implementation for the Paper platform.
///
/// Performs the platform-specific narrowing cast from the generic
/// `<P extends Audience & Identified>` type parameter to
/// [org.bukkit.entity.Player], confining that knowledge to this single
/// class so that neither [io.github.namiuni.paperplugintemplate.common.user.UserServiceInternal]
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

    private final ComponentStore registry;

    @Inject
    public PaperUserFactory(final ComponentStore registry) {
        this.registry = registry;
    }

    /// {@inheritDoc}
    ///
    /// Registers [IdentityComponent], [PersistenceComponent], and
    /// [PaperPlayerComponent] into the shared registry before
    /// constructing the stateless [PaperUser] view.
    ///
    /// @throws ClassCastException if `player` is not a [Player]; indicates
    ///         a misconfigured Guice binding for a non-Paper platform
    @Override
    public <P extends Audience & Identified> UserInternal create(final P player, final UserRecord profile) {
        final Player bukkit = (Player) player;
        final UUID uuid = bukkit.getUniqueId();

        this.registry.set(uuid, ComponentTypes.PLAYER, new PaperPlayerComponent(bukkit));

        return new PaperUser(uuid, this.registry);
    }
}

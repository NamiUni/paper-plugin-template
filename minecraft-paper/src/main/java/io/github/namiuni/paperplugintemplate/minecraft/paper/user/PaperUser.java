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

import io.github.namiuni.paperplugintemplate.common.component.ComponentRegistry;
import io.github.namiuni.paperplugintemplate.common.component.ComponentTypes;
import io.github.namiuni.paperplugintemplate.common.component.components.PlayerComponent;
import io.github.namiuni.paperplugintemplate.common.user.UserInternal;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.ForwardingAudience;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/// Paper-specific [UserInternal] adapter that wraps a live
/// [org.bukkit.entity.Player].
///
/// All live-data access — online status, display name, locale, and any
/// future additions such as health or inventory — delegates directly to
/// the underlying [org.bukkit.entity.Player], eliminating the supplier-based workarounds
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
/// The persistent [io.github.namiuni.paperplugintemplate.common.infrastructure.persistence.UserRecord] is held in an [java.util.concurrent.atomic.AtomicReference],
/// enabling lock-free copy-on-write updates from any thread. No
/// `synchronized` blocks are used; this class is therefore free from
/// virtual-thread carrier-thread pinning (JEP 491).
///
/// Live-data methods such as [#isOnline()] and [#displayName()] delegate
/// to the underlying [org.bukkit.entity.Player], whose thread-safety is governed by the
/// Paper API contract.
@NullMarked
public final class PaperUser implements UserInternal, ForwardingAudience.Single {

    private final UUID uuid;
    private final ComponentRegistry registry;

    /// Constructs a new view bound to `entityId` and `registry`.
    ///
    /// @param uuid the entity identifier; must not be `null`
    /// @param registry the shared component registry; must not be `null`
    public PaperUser(final UUID uuid, final ComponentRegistry registry) {
        this.uuid = Objects.requireNonNull(uuid, "uuid");
        this.registry = Objects.requireNonNull(registry, "registry");
    }

    // -------------------------------------------------------------------------
    // UserInternal — component accessors and mutators
    // -------------------------------------------------------------------------

    /// {@inheritDoc}
    @Override
    public PlayerComponent player() {
        return this.registry.getOrThrow(this.uuid, ComponentTypes.PLAYER);
    }

    // -------------------------------------------------------------------------
    // PluginTemplateUser
    // -------------------------------------------------------------------------

    @Override
    public UUID uuid() {
        return this.uuid;
    }

    @Override
    public String name() {
        return this.get(Identity.NAME).orElseThrow();
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
        return this.player().lastSeen();
    }

    @Override
    public boolean isOnline() {
        return this.player().isOnline();
    }

    // -------------------------------------------------------------------------
    // ForwardingAudience.Single + Identified
    // -------------------------------------------------------------------------

    @Override
    public Audience audience() {
        return this.player().audience();
    }

    @Override
    public Identity identity() {
        return Identity.identity(this.uuid);
    }

    // -------------------------------------------------------------------------
    // Object
    // -------------------------------------------------------------------------

    @Override
    public String toString() {
        return "PaperUser{uuid=%s, registry=%s}".formatted(this.uuid, this.registry);
    }

    @Override
    public boolean equals(final @Nullable Object that) {
        if (that == null || getClass() != that.getClass()) {
            return false;
        }
        final PaperUser paperUser = (PaperUser) that;
        return Objects.equals(this.uuid, paperUser.uuid) && Objects.equals(this.registry, paperUser.registry);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.uuid, this.registry);
    }
}

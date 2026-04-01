/*
 * PaperPluginTemplate
 *
 * Copyright (c) 2026. Namiu (ãã«ããã)
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
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.ForwardingAudience;
import net.kyori.adventure.identity.Identified;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import org.jspecify.annotations.NullMarked;

@NullMarked
public final class PlatformUser<P extends Audience & Identified> implements PluginTemplateUser, ForwardingAudience.Single {

    private final Audience player;
    private final AtomicReference<UserProfile> profile;

    public PlatformUser(final P player, final UserProfile profile) {
        this.player = player;
        this.profile = new AtomicReference<>(profile);
    }

    UserProfile profile() {
        return this.profile.get();
    }

    @Override
    public UUID uuid() {
        return this.get(Identity.UUID).orElseThrow();
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
        return this.profile.get().lastSeen();
    }

    @Override
    public void lastSeen(Instant instant) {
        this.profile.getAndUpdate(old -> old.withLastSeen(instant));
    }

    @Override
    public Identity identity() {
        return Identity.identity(this.uuid());
    }

    @Override
    public Audience audience() {
        return this.player;
    }
}

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
package io.github.namiuni.paperplugintemplate.api.user;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.identity.Identified;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NullMarked;

//TODO: Javadoc
@NullMarked
@ApiStatus.NonExtendable
public interface PluginTemplateUser extends Audience, Identified {

    //TODO: Javadoc
    UUID uuid();

    //TODO: Javadoc
    String name();

    //TODO: Javadoc
    Component displayName();

    //TODO: Javadoc
    Locale locale();

    //TODO: Javadoc
    Instant lastSeen();

    void lastSeen(Instant instant);
}

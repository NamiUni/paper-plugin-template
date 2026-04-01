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

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.identity.Identified;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NullMarked;

//TODO: Javadoc
@NullMarked
@ApiStatus.NonExtendable
public interface PluginTemplateUserService {

    //TODO: Javadoc
    <P extends Audience & Identified> Optional<PluginTemplateUser> getUser(P player);

    //TODO: Javadoc
    <P extends Audience & Identified> CompletableFuture<PluginTemplateUser> loadUser(P player);

    //TODO: Javadoc
    CompletableFuture<Void> deleteUser(UUID uuid);
}

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
package io.github.namiuni.paperplugintemplate.minecraft.sponge;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import io.github.namiuni.paperplugintemplate.api.user.PluginTemplateUserService;
import io.github.namiuni.paperplugintemplate.common.command.CommandSource;
import io.github.namiuni.paperplugintemplate.common.user.UserFactory;
import io.github.namiuni.paperplugintemplate.minecraft.sponge.command.SpongeCommandSource;
import io.github.namiuni.paperplugintemplate.minecraft.sponge.user.SpongeUserFactory;
import jakarta.inject.Singleton;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.SenderMapper;
import org.jspecify.annotations.NullMarked;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.plugin.PluginContainer;

@NullMarked
@SuppressWarnings("unused")
final class SpongeModule extends AbstractModule {

    private final PluginContainer container;

    SpongeModule(final PluginContainer container) {
        this.container = container;
    }

    @Provides
    @Singleton
    private CommandManager<CommandSource> commandManager(final PluginTemplateUserService userService) {
        final SenderMapper<CommandCause, CommandSource> senderMapper = SenderMapper.create(
                cause -> new SpongeCommandSource(cause, userService),
                source -> ((SpongeCommandSource) source).spongeCause()
        );

        // Where's the sponge cloud!?!?!?!?!?!?!?!?
    }

    @Override
    protected void configure() {
        this.bind(UserFactory.class).to(SpongeUserFactory.class).in(Scopes.SINGLETON);
    }
}

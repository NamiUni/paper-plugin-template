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
package io.github.namiuni.paperplugintemplate.common;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.multibindings.Multibinder;
import io.github.namiuni.paperplugintemplate.api.PluginTemplate;
import io.github.namiuni.paperplugintemplate.api.user.PluginTemplateUserService;
import io.github.namiuni.paperplugintemplate.common.command.commands.CommandFactory;
import io.github.namiuni.paperplugintemplate.common.command.commands.HelpCommand;
import io.github.namiuni.paperplugintemplate.common.command.commands.ReloadCommand;
import io.github.namiuni.paperplugintemplate.common.event.EventBus;
import io.github.namiuni.paperplugintemplate.common.event.SimpleEventBus;
import io.github.namiuni.paperplugintemplate.common.user.UserServiceInternal;
import io.github.namiuni.paperplugintemplate.common.utilities.gson.serializations.InstantTypeAdapter;
import io.github.namiuni.paperplugintemplate.common.utilities.gson.serializations.UUIDTypeAdapter;
import java.time.Instant;
import java.util.UUID;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.jspecify.annotations.NullMarked;

@NullMarked
@SuppressWarnings("unused")
public final class CommonModule extends AbstractModule {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(Instant.class, InstantTypeAdapter.INSTANCE)
            .registerTypeAdapter(UUID.class, UUIDTypeAdapter.INSTANCE)
            .create();

    // JIS Z 9103 https://ja.wikipedia.org/wiki/JIS%E5%AE%89%E5%85%A8%E8%89%B2
    private static final TextColor RED = TextColor.color(0xFF4B00);
    private static final TextColor YELLOW = TextColor.color(0xF2E700);
    private static final TextColor GREEN = TextColor.color(0x00B06B);
    private static final TextColor BLUE = TextColor.color(0x1971FF);
    private static final MiniMessage MINI_MESSAGE = MiniMessage.builder()
            .tags(TagResolver.builder()
                    .resolver(TagResolver.standard())
                    .tag("error", Tag.styling(RED))
                    .tag("warn", Tag.styling(YELLOW))
                    .tag("info", Tag.styling(GREEN))
                    .tag("debug", Tag.styling(BLUE))
                    .build())
            .build();

    private final Metadata metadata;

    public CommonModule(final Metadata metadata) {
        this.metadata = metadata;
    }

    @Override
    protected void configure() {
        this.bind(Metadata.class).toInstance(this.metadata);
        this.bind(EventBus.class).to(SimpleEventBus.class).in(Scopes.SINGLETON);
        this.bind(PluginTemplateUserService.class).to(UserServiceInternal.class).in(Scopes.SINGLETON);
        this.bind(PluginTemplate.class).to(PluginTemplateImpl.class).in(Scopes.SINGLETON);

        this.bind(Gson.class).toInstance(GSON);
        this.bind(MiniMessage.class).toInstance(MINI_MESSAGE);

        this.bindCommands();
    }

    private void bindCommands() {
        final Multibinder<CommandFactory> commands = Multibinder.newSetBinder(this.binder(), CommandFactory.class);
        commands.addBinding().to(ReloadCommand.class).in(Scopes.SINGLETON);
        commands.addBinding().to(HelpCommand.class).in(Scopes.SINGLETON);
    }
}

/*
 * plugin-template
 *
 * Copyright (c) 2024. Namiu (Unitarou)
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

package com.github.namiuni.plugintemplate;

import com.github.namiuni.plugintemplate.command.BaseCommand;
import com.github.namiuni.plugintemplate.command.commands.ReloadCommand;
import com.github.namiuni.plugintemplate.message.AudienceReceiverResolver;
import com.github.namiuni.plugintemplate.message.MessageRenderer;
import com.github.namiuni.plugintemplate.message.MessageService;
import com.github.namiuni.plugintemplate.message.TranslatableMessageSource;
import com.github.namiuni.plugintemplate.message.placeholders.ComponentPlaceholderResolver;
import com.github.namiuni.plugintemplate.message.placeholders.StringPlaceholderResolver;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import io.leangen.geantyref.TypeToken;
import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.configuration.PluginMeta;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import net.kyori.moonshine.Moonshine;
import net.kyori.moonshine.exception.scan.UnscannableMethodException;
import net.kyori.moonshine.strategy.StandardPlaceholderResolverStrategy;
import net.kyori.moonshine.strategy.supertype.StandardSupertypeThenInterfaceSupertypeStrategy;
import org.bukkit.event.Listener;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;

import java.nio.file.Path;

@DefaultQualifier(NonNull.class)
@SuppressWarnings("UnstableApiUsage")
public final class BootstrapModule extends AbstractModule {

    private final BootstrapContext bootstrapContext;

    public BootstrapModule(final BootstrapContext bootstrapContext) {
        this.bootstrapContext = bootstrapContext;
    }

    @Provides
    @Singleton
    public MessageService messageService(
            final AudienceReceiverResolver audienceReceiverResolver,
            final TranslatableMessageSource messageSource,
            final MessageRenderer messageRenderer,
            final ComponentPlaceholderResolver componentPlaceholderResolver,
            final StringPlaceholderResolver stringPlaceholderResolver
    ) throws UnscannableMethodException {
        return Moonshine.<MessageService, Audience>builder(new TypeToken<>() {})
                .receiverLocatorResolver(audienceReceiverResolver, 0)
                .sourced(messageSource)
                .rendered(messageRenderer)
                .sent(Audience::sendMessage)
                .resolvingWithStrategy(new StandardPlaceholderResolverStrategy<>(new StandardSupertypeThenInterfaceSupertypeStrategy(false)))
                .weightedPlaceholderResolver(Component.class, componentPlaceholderResolver, 0)
                .weightedPlaceholderResolver(String.class, stringPlaceholderResolver, 0)
                .create(this.getClass().getClassLoader());
    }

    @Override
    protected void configure() {
        this.bind(PluginMeta.class).toInstance(this.bootstrapContext.getPluginMeta());
        this.bind(Path.class).annotatedWith(DataDirectory.class).toInstance(this.bootstrapContext.getDataDirectory());
        this.bind(ComponentLogger.class).toInstance(this.bootstrapContext.getLogger());

        this.configureListeners();
        this.configureCommands();
    }

    private void configureListeners() {
        final var listeners = Multibinder.newSetBinder(this.binder(), Listener.class);
//        listeners.addBinding().to(YourListener.class).in(Scopes.SINGLETON);
    }

    private void configureCommands() {
        final var commands = Multibinder.newSetBinder(this.binder(), BaseCommand.class);
        commands.addBinding().to(ReloadCommand.class).in(Scopes.SINGLETON);
    }
}

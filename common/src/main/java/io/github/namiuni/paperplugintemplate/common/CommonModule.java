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
package io.github.namiuni.paperplugintemplate.common;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import io.github.namiuni.kotonoha.translatable.message.KotonohaMessage;
import io.github.namiuni.kotonoha.translatable.message.configuration.FormatTypes;
import io.github.namiuni.kotonoha.translatable.message.policy.argument.TranslationArgumentAdaptationPolicy;
import io.github.namiuni.kotonoha.translatable.message.policy.argument.tag.TagNameResolver;
import io.github.namiuni.kotonoha.translatable.message.utility.TranslationArgumentAdapter;
import io.github.namiuni.paperplugintemplate.api.user.PluginTemplateUserService;
import io.github.namiuni.paperplugintemplate.common.configuration.ConfigurationLoader;
import io.github.namiuni.paperplugintemplate.common.configuration.PrimaryConfiguration;
import io.github.namiuni.paperplugintemplate.common.translation.Messages;
import io.github.namiuni.paperplugintemplate.common.user.PluginTemplateUserServiceInternal;
import jakarta.inject.Singleton;
import java.nio.file.Path;
import org.jspecify.annotations.NullMarked;

@NullMarked
public final class CommonModule extends AbstractModule {

    public CommonModule() {
    }

    /// Provides a singleton [ConfigurationLoader] for [PrimaryConfiguration].
    ///
    /// @param dataDirectory the plugin data directory
    /// @return a fully constructed configuration loader
    @Provides
    @Singleton
    private ConfigurationLoader<PrimaryConfiguration> primaryConfigLoader(
            final @DataDirectory Path dataDirectory
    ) {
        return new ConfigurationLoader<>(
                PrimaryConfiguration.class,
                PrimaryConfiguration.DEFAULT,
                dataDirectory
        );
    }

    /// Provides a singleton [Messages] proxy backed by Kotonoha.
    ///
    /// @return a proxied implementation of [Messages]
    @Provides
    @Singleton
    private Messages translations() {
        final var argumentPolicy = TranslationArgumentAdaptationPolicy.miniMessage(
                TranslationArgumentAdapter.standard(),
                TagNameResolver.annotationOrParameterNameResolver()
        );
        final var config = FormatTypes.MINI_MESSAGE.withArgumentPolicy(argumentPolicy);
        return KotonohaMessage.createProxy(Messages.class, config);
    }

    @Override
    protected void configure() {
        this.bind(PluginTemplateUserService.class).to(PluginTemplateUserServiceInternal.class);
    }
}

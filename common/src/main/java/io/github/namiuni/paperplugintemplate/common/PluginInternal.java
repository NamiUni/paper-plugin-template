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

import io.github.namiuni.paperplugintemplate.api.PluginTemplate;
import io.github.namiuni.paperplugintemplate.api.PluginTemplateProvider;
import io.github.namiuni.paperplugintemplate.api.user.PluginTemplateUserService;
import io.github.namiuni.paperplugintemplate.common.translation.TranslatorHolder;
import io.github.namiuni.paperplugintemplate.common.user.storage.UserRepository;
import jakarta.inject.Inject;
import net.kyori.adventure.translation.GlobalTranslator;
import org.jspecify.annotations.NullMarked;

@NullMarked
public final class PluginInternal implements PluginTemplate {

    private final TranslatorHolder translatorHolder;
    private final UserRepository userRepository;
    private final PluginTemplateUserService userService;

    @Inject
    private PluginInternal(
            final TranslatorHolder translatorHolder,
            final UserRepository userRepository,
            final PluginTemplateUserService userService
    ) {
        this.translatorHolder = translatorHolder;
        this.userRepository = userRepository;
        this.userService = userService;
    }

    @Override
    public PluginTemplateUserService userService() {
        return this.userService;
    }

    /// Performs one-time initialization tasks that must run before the server starts.
    public void initialize() {
        // Translator
        GlobalTranslator.translator().addSource(this.translatorHolder.get());

        // Repository
        this.userRepository.initialize();

        PluginTemplateProvider.register(this);
    }
}

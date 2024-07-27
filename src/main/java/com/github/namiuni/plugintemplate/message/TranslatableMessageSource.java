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

package com.github.namiuni.plugintemplate.message;

import com.github.namiuni.plugintemplate.config.ConfigManager;
import com.google.inject.Inject;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.identity.Identity;
import net.kyori.moonshine.exception.MissingMessageException;
import net.kyori.moonshine.message.IMessageSource;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;

import java.text.MessageFormat;

@DefaultQualifier(NonNull.class)
public final class TranslatableMessageSource implements IMessageSource<Audience, String> {

    private final ConfigManager configManager;
    private final TranslationManager translationManager;

    @Inject
    public TranslatableMessageSource(
            final ConfigManager configManager,
            final TranslationManager translationManager
    ) {
        this.configManager = configManager;
        this.translationManager = translationManager;
    }

    @Override
    public String messageOf(final Audience receiver, final String messageKey) throws MissingMessageException {
        final var locale = receiver.pointers().getOrDefault(Identity.LOCALE, configManager.primaryConfig().defaultLocale());
        final @Nullable MessageFormat translated = this.translationManager.translate(messageKey, locale);
        if (translated != null) {
            return translated.toPattern();
        } else {
            throw new MissingMessageException(messageKey);
        }
    }
}

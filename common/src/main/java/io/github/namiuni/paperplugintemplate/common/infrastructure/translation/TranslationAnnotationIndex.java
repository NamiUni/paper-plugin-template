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
package io.github.namiuni.paperplugintemplate.common.infrastructure.translation;

import io.github.namiuni.kotonoha.annotations.Key;
import io.github.namiuni.kotonoha.annotations.Message;
import io.github.namiuni.paperplugintemplate.common.infrastructure.translation.translations.MessageAssembly;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.jspecify.annotations.NullMarked;

@NullMarked
final class TranslationAnnotationIndex {

    private TranslationAnnotationIndex() {
    }

    static Map<Locale, List<Translation.Message>> build() {
        final Map<Locale, List<Translation.Message>> index = new LinkedHashMap<>();

        Arrays.stream(MessageAssembly.class.getMethods())
                .filter(method -> method.isAnnotationPresent(Key.class))
                .sorted(Comparator.comparing(method -> method.getAnnotation(Key.class).value()))
                .forEach(method -> {
                    final String key = method.getAnnotation(Key.class).value();
                    for (final Message annotation : method.getAnnotationsByType(Message.class)) {
                        index.computeIfAbsent(annotation.locale().asLocale(), _ -> new ArrayList<>())
                                .add(new Translation.Message(key, annotation.content()));
                    }
                });

        return index;
    }
}

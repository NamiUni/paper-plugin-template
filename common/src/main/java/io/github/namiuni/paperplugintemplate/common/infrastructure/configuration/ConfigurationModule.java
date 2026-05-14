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
package io.github.namiuni.paperplugintemplate.common.infrastructure.configuration;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import io.github.namiuni.paperplugintemplate.common.infrastructure.configuration.serializer.ResourcePackInfoSerializer;
import io.github.namiuni.paperplugintemplate.common.infrastructure.configuration.serializer.ResourcePackRequestSerializer;
import jakarta.inject.Singleton;
import net.kyori.adventure.resource.ResourcePackInfo;
import net.kyori.adventure.resource.ResourcePackRequest;
import net.kyori.adventure.serializer.configurate4.ConfigurateComponentSerializer;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jspecify.annotations.NullMarked;
import org.spongepowered.configurate.serialize.TypeSerializerCollection;

@NullMarked
public final class ConfigurationModule extends AbstractModule {

    @Provides
    @Singleton
    @SuppressWarnings("unused")
    TypeSerializerCollection typeSerializers(final MiniMessage miniMessage) {
        final TypeSerializerCollection kyori = ConfigurateComponentSerializer.builder()
                .scalarSerializer(miniMessage)
                .build()
                .serializers();

        return TypeSerializerCollection.builder()
                .registerAll(kyori)
                .register(ResourcePackInfo.class, ResourcePackInfoSerializer.INSTANCE)
                .register(ResourcePackRequest.class, ResourcePackRequestSerializer.INSTANCE)
                .build();
    }
}

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
package io.github.namiuni.paperplugintemplate.common.utilities.gson.serializations;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import jakarta.inject.Inject;
import java.lang.reflect.Type;
import java.util.UUID;
import org.jspecify.annotations.NullMarked;

@NullMarked
public final class UUIDTypeAdapter implements JsonSerializer<UUID>, JsonDeserializer<UUID> {

    public static final UUIDTypeAdapter INSTANCE = new UUIDTypeAdapter();

    @Inject
    private UUIDTypeAdapter() {
    }

    @Override
    public JsonElement serialize(final UUID src, final Type typeOfSrc, final JsonSerializationContext context) {
        return new JsonPrimitive(src.toString());
    }

    @Override
    public UUID deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context) throws JsonParseException {
        return UUID.fromString(json.getAsString());
    }
}

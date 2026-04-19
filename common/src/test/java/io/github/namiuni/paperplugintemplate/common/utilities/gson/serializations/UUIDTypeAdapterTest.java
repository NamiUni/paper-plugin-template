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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import java.util.UUID;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

@NullMarked
class UUIDTypeAdapterTest {

    private static final UUIDTypeAdapter ADAPTER = UUIDTypeAdapter.INSTANCE;
    private static final JsonSerializationContext SERIALIZE_CTX = mock(JsonSerializationContext.class);
    private static final JsonDeserializationContext DESERIALIZE_CTX = mock(JsonDeserializationContext.class);

    // ── serialize ─────────────────────────────────────────────────────────────

    @Test
    void serializeProducesStringRepresentation() {
        final UUID uuid = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

        final var result = (JsonPrimitive) ADAPTER.serialize(uuid, UUID.class, SERIALIZE_CTX);

        assertEquals("550e8400-e29b-41d4-a716-446655440000", result.getAsString());
    }

    @Test
    void serializeNilUUIDProducesAllZerosString() {
        final UUID nil = new UUID(0L, 0L);

        final var result = (JsonPrimitive) ADAPTER.serialize(nil, UUID.class, SERIALIZE_CTX);

        assertEquals("00000000-0000-0000-0000-000000000000", result.getAsString());
    }

    // ── deserialize ───────────────────────────────────────────────────────────

    @Test
    void deserializeReconstructsUUIDFromString() {
        final String uuidString = "550e8400-e29b-41d4-a716-446655440000";

        final UUID result = ADAPTER.deserialize(new JsonPrimitive(uuidString), UUID.class, DESERIALIZE_CTX);

        assertEquals(UUID.fromString(uuidString), result);
    }

    // ── roundtrip ─────────────────────────────────────────────────────────────

    @RepeatedTest(10)
    void roundtripPreservesRandomUUID() {
        final UUID original = UUID.randomUUID();

        final var json = ADAPTER.serialize(original, UUID.class, SERIALIZE_CTX);
        final UUID restored = ADAPTER.deserialize(json, UUID.class, DESERIALIZE_CTX);

        assertEquals(original, restored);
    }
}

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
import java.time.Instant;
import org.junit.jupiter.api.Test;

class InstantTypeAdapterTest {

    private static final InstantTypeAdapter ADAPTER = InstantTypeAdapter.INSTANCE;
    private static final JsonSerializationContext SERIALIZE_CTX = mock(JsonSerializationContext.class);
    private static final JsonDeserializationContext DESERIALIZE_CTX = mock(JsonDeserializationContext.class);

    // ── serialize ─────────────────────────────────────────────────────────────

    @Test
    void serializeReturnsEpochMilliAsPrimitive() {
        final Instant instant = Instant.ofEpochMilli(1_700_000_000_000L);

        final var result = (JsonPrimitive) ADAPTER.serialize(instant, Instant.class, SERIALIZE_CTX);

        assertEquals(1_700_000_000_000L, result.getAsLong());
    }

    @Test
    void serializeEpochZeroProducesZero() {
        final var result = (JsonPrimitive) ADAPTER.serialize(Instant.EPOCH, Instant.class, SERIALIZE_CTX);

        assertEquals(0L, result.getAsLong());
    }

    @Test
    void serializePreservesSubSecondPrecisionAsMillis() {
        final Instant instant = Instant.ofEpochSecond(1L, 500_000_000L); // 1.5 seconds
        final var result = (JsonPrimitive) ADAPTER.serialize(instant, Instant.class, SERIALIZE_CTX);

        assertEquals(1_500L, result.getAsLong());
    }

    // ── deserialize ───────────────────────────────────────────────────────────

    @Test
    void deserializeReconstructsInstantFromEpochMilli() {
        final long millis = 1_700_000_000_000L;

        final Instant result = ADAPTER.deserialize(new JsonPrimitive(millis), Instant.class, DESERIALIZE_CTX);

        assertEquals(Instant.ofEpochMilli(millis), result);
    }

    @Test
    void deserializeZeroProducesEpoch() {
        final Instant result = ADAPTER.deserialize(new JsonPrimitive(0L), Instant.class, DESERIALIZE_CTX);

        assertEquals(Instant.EPOCH, result);
    }

    // ── roundtrip ─────────────────────────────────────────────────────────────

    @Test
    void roundtripPreservesArbitraryInstant() {
        final Instant original = Instant.ofEpochMilli(1_234_567_890_123L);

        final var json = ADAPTER.serialize(original, Instant.class, SERIALIZE_CTX);
        final Instant restored = ADAPTER.deserialize(json, Instant.class, DESERIALIZE_CTX);

        assertEquals(original, restored);
    }
}

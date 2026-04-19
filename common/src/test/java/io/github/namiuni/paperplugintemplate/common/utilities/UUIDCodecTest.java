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
package io.github.namiuni.paperplugintemplate.common.utilities;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.UUID;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

@NullMarked
class UUIDCodecTest {

    @Test
    void encodedBytesAreAlways16Bytes() {
        assertEquals(16, UUIDCodec.uuidToBytes(UUID.randomUUID()).length);
    }

    @RepeatedTest(20)
    void roundtripPreservesRandomUUID() {
        final UUID original = UUID.randomUUID();
        assertEquals(original, UUIDCodec.uuidFromBytes(UUIDCodec.uuidToBytes(original)));
    }

    @Test
    void roundtripPreservesZeroUUID() {
        final UUID zero = new UUID(0L, 0L);
        assertEquals(zero, UUIDCodec.uuidFromBytes(UUIDCodec.uuidToBytes(zero)));
    }

    @Test
    void roundtripPreservesMaxBitsUUID() {
        final UUID max = new UUID(Long.MAX_VALUE, Long.MIN_VALUE);
        assertEquals(max, UUIDCodec.uuidFromBytes(UUIDCodec.uuidToBytes(max)));
    }

    @Test
    void mostSignificantBitsStoredBigEndianInFirstEightBytes() {
        final UUID uuid = new UUID(0x0102030405060708L, 0L);
        final byte[] bytes = UUIDCodec.uuidToBytes(uuid);

        assertArrayEquals(
                new byte[] {0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08},
                Arrays.copyOf(bytes, 8)
        );
    }

    @Test
    void leastSignificantBitsStoredBigEndianInLastEightBytes() {
        final UUID uuid = new UUID(0L, 0x0807060504030201L);
        final byte[] bytes = UUIDCodec.uuidToBytes(uuid);

        assertArrayEquals(
                new byte[] {0x08, 0x07, 0x06, 0x05, 0x04, 0x03, 0x02, 0x01},
                Arrays.copyOfRange(bytes, 8, 16)
        );
    }

    @Test
    void knownUUIDProducesExpectedBytes() {
        // 00000000-0000-0000-0000-000000000001
        final UUID uuid = new UUID(0L, 1L);
        final byte[] bytes = UUIDCodec.uuidToBytes(uuid);
        final byte[] expected = new byte[] {
                0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 1
        };
        assertArrayEquals(expected, bytes);
    }
}

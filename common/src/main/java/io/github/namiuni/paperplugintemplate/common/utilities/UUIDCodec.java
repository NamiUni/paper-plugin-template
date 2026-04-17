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

import java.util.UUID;
import org.jspecify.annotations.NullMarked;

@NullMarked
public final class UUIDCodec {

    private UUIDCodec() {
    }

    public static byte[] uuidToBytes(final UUID uuid) {
        final long msb = uuid.getMostSignificantBits();
        final long lsb = uuid.getLeastSignificantBits();
        return new byte[] {
                (byte) (msb >>> 56), (byte) (msb >>> 48), (byte) (msb >>> 40), (byte) (msb >>> 32),
                (byte) (msb >>> 24), (byte) (msb >>> 16), (byte) (msb >>> 8), (byte) msb,
                (byte) (lsb >>> 56), (byte) (lsb >>> 48), (byte) (lsb >>> 40), (byte) (lsb >>> 32),
                (byte) (lsb >>> 24), (byte) (lsb >>> 16), (byte) (lsb >>> 8), (byte) lsb,
        };
    }

    public static UUID uuidFromBytes(final byte[] b) {
        long msb = 0L;
        long lsb = 0L;
        for (int i = 0; i < 8; i++) {
            msb = (msb << 8) | (b[i] & 0xFFL);
        }
        for (int i = 8; i < 16; i++) {
            lsb = (lsb << 8) | (b[i] & 0xFFL);
        }
        return new UUID(msb, lsb);
    }
}

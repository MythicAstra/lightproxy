/*
 * Copyright (C) 2024 sharedwonder (Liu Baihao).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.sharedwonder.mc.ptbridge.packet;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;

public final class PacketUtils {
    private PacketUtils() {}

    public static final int VARINT_MAX_SIZE = 5;

    public static int calcVarintSize(int value) {
        var shift = 7;
        for (var result = 1; result < VARINT_MAX_SIZE; ++result) {
            if ((value & (0xffffffff << shift)) == 0) {
                return result;
            }
            shift += 7;
        }
        return VARINT_MAX_SIZE;
    }

    public static byte[] readBytes(ByteBuf buffer) {
        return readBytes(buffer, buffer.readableBytes());
    }

    public static byte[] readBytes(ByteBuf buffer, int size) {
        var bytes = new byte[size];
        buffer.readBytes(bytes);
        return bytes;
    }

    public static void skipChunk(ByteBuf buffer) {
        buffer.skipBytes(checkChunkSize(readVarint(buffer)));
    }

    public static void skipVarint(ByteBuf buffer) {
        for (var counter = 0; counter < VARINT_MAX_SIZE; ++counter) {
            var b = buffer.readByte();
            if ((b & 0x80) == 0) {
                return;
            }
        }

        throw new DecoderException("Invalid varint");
    }

    public static int readVarint(ByteBuf buffer) {
        var result = 0;
        var shift = 0;

        for (var counter = 0; counter < VARINT_MAX_SIZE; ++counter) {
            var b = buffer.readByte();
            result |= (b & 0x7f) << shift;
            if ((b & 0x80) == 0) {
                return result;
            }
            shift += 7;
        }

        throw new DecoderException("Invalid varint");
    }

    public static void writeVarint(ByteBuf buffer, int value) {
        var input = value;

        while ((input & 0xffffff80) != 0) {
            buffer.writeByte((byte) ((input & 0x7f) | 0x80));
            input >>>= 7;
        }

        buffer.writeByte((byte) input);
    }

    public static byte[] readByteArray(ByteBuf buffer) {
        return readBytes(buffer, checkChunkSize(readVarint(buffer)));
    }

    public static void writeByteArray(ByteBuf buffer, byte[] bytes) {
        writeVarint(buffer, bytes.length);
        buffer.writeBytes(bytes);
    }

    public static String readUtf8String(ByteBuf buffer) {
        return new String(readByteArray(buffer), StandardCharsets.UTF_8);
    }

    public static void writeUtf8String(ByteBuf buffer, String string) {
        writeByteArray(buffer, string.getBytes(StandardCharsets.UTF_8));
    }

    public static UUID readUuid(ByteBuf buffer) {
        return new UUID(buffer.readLong(), buffer.readLong());
    }

    public static void writeUuid(ByteBuf buffer, UUID uuid) {
        buffer.writeLong(uuid.getMostSignificantBits());
        buffer.writeLong(uuid.getLeastSignificantBits());
    }

    private static int checkChunkSize(int size) {
        if (size < 0) {
            throw new DecoderException("Invalid buffer chunk size (less than 0): " + size);
        }
        return size;
    }
}

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

import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public final class PacketCompressionUtils {
    private PacketCompressionUtils() {}

    private static final Deflater DEFLATER = new Deflater();

    private static final Inflater INFLATER = new Inflater();

    public static void compress(int compressionThreshold, int size, ByteBuf in, ByteBuf out) {
        if (size >= compressionThreshold) {
            var buf = Unpooled.buffer();
            var bytes = PacketUtils.readBytes(in, size);
            PacketUtils.writeVarint(buf, size);

            DEFLATER.setInput(bytes);
            DEFLATER.finish();
            var deflateBuffer = new byte[8192];
            while (!DEFLATER.finished()) {
                var outCompressedSize = DEFLATER.deflate(deflateBuffer);
                buf.writeBytes(deflateBuffer, 0, outCompressedSize);
            }
            DEFLATER.reset();

            PacketUtils.writeVarint(out, buf.readableBytes());
            out.writeBytes(buf);
            buf.release();
        } else {
            PacketUtils.writeVarint(out, size + 1);
            PacketUtils.writeVarint(out, 0);
            out.writeBytes(in);
        }
    }

    public static int decompress(int size, ByteBuf in, ByteBuf out) {
        var before = in.readerIndex();
        var decompressedSize = PacketUtils.readVarint(in);
        var originalSize = size - (in.readerIndex() - before);

        if (decompressedSize > 0) {
            var compressedBytes = PacketUtils.readBytes(in, originalSize);

            INFLATER.setInput(compressedBytes);
            var decompressedBytes = new byte[decompressedSize];
            try {
                INFLATER.inflate(decompressedBytes);
            } catch (DataFormatException exception) {
                throw new RuntimeException("Failed to decompress packet", exception);
            }
            INFLATER.reset();

            out.writeBytes(decompressedBytes);
            return decompressedSize;
        }

        out.writeBytes(in, originalSize);
        return originalSize;
    }
}

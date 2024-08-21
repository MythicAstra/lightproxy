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

package net.sharedwonder.lightproxy;

import javax.annotation.Nullable;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import net.sharedwonder.lightproxy.crypt.EncryptionContext;
import net.sharedwonder.lightproxy.packet.HandledFlag;
import net.sharedwonder.lightproxy.packet.PacketCompressionUtils;
import net.sharedwonder.lightproxy.packet.PacketType;
import net.sharedwonder.lightproxy.packet.PacketUtils;

abstract sealed class ProxyChannelHandler extends ChannelInboundHandlerAdapter permits ProxyBackendHandler, ProxyServerHandler {
    final ConnectionContext connectionContext;

    private final PacketType packetType;

    @Nullable
    private ByteBuf buffer;

    private int remainingUnreadSize = 0;

    ProxyChannelHandler(ConnectionContext connectionContext, PacketType packetType) {
        this.connectionContext = connectionContext;
        this.packetType = packetType;
    }

    abstract ChannelFuture sendMessage(ByteBuf message);

    @Override
    public void channelRead(ChannelHandlerContext context, Object message) throws Exception {
        var in = (ByteBuf) message;
        try {
            if (!in.isReadable()) {
                return;
            }

            var encryptionContext = connectionContext.getEncryptionContext();
            var messageBuffer = encryptionContext.isEnabled() ? encryptionContext.decrypt(packetType, in) : in;
            var allocator = context.alloc();

            if (remainingUnreadSize > 0) {
                var available = messageBuffer.readableBytes();
                assert buffer != null; 
                buffer.writeBytes(messageBuffer);
                remainingUnreadSize -= available;
                if (remainingUnreadSize > 0) {
                    return;
                }
            } else {
                buffer = allocator.heapBuffer();
                buffer.writeBytes(messageBuffer);
            }

            var out = allocator.heapBuffer();
            var complete = true;
            while (buffer.isReadable()) {
                if (!handle(allocator, out)) {
                    complete = false;
                    break;
                }
            }

            if (packetType == PacketType.S2C) {
                var attachedS2CPackets = connectionContext.getAttachedS2CPackets();
                while (!attachedS2CPackets.isEmpty()) {
                    var packet = attachedS2CPackets.poll();
                    if (connectionContext.isEnabledCompressionForClient()) {
                        PacketCompressionUtils.compress(connectionContext.getCompressionThreshold(), PacketUtils.readVarint(packet), packet, out);
                        packet.release();
                    } else {
                        out.writeBytes(packet);
                    }
                }
            } else if (packetType == PacketType.C2S) {
                var attachedC2SPackets = connectionContext.getAttachedC2SPackets();
                while (!attachedC2SPackets.isEmpty()) {
                    var packet = attachedC2SPackets.poll();
                    PacketCompressionUtils.compress(connectionContext.getCompressionThreshold(), PacketUtils.readVarint(packet), packet, out);
                    packet.release();
                }
            } else {
                throw new AssertionError();
            }

            send(encryptionContext, out);
            if (complete) {
                buffer.release();
            }
        } finally {
            in.release();
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext context) {
        if (buffer != null && buffer.refCnt() > 0) {
            remainingUnreadSize = 0;
            buffer.release();
        }
    }

    private void send(EncryptionContext encryptionContext, ByteBuf message) {
        ByteBuf out;
        if (encryptionContext.isEnabled()) {
            out = encryptionContext.encrypt(packetType, message);
            message.release();
        } else {
            out = message;
        }
        sendMessage(out).addListener((ChannelFutureListener) listener -> {
            if (!listener.isSuccess()) {
                listener.channel().close();
            }
        });
    }

    private boolean handle(ByteBufAllocator allocator, ByteBuf out) throws Exception {
        assert buffer != null; 
        var startIndex = buffer.readerIndex();
        int size;
        try {
            size = PacketUtils.readVarint(buffer);
        } catch (IndexOutOfBoundsException exception) {
            remainingUnreadSize = 1;
            buffer.readerIndex(startIndex);

            return false;
        }

        var endIndex = buffer.readerIndex() + size;
        var available = buffer.readableBytes();
        if (size > available) {
            remainingUnreadSize = size - available;
            buffer.readerIndex(startIndex);

            return false;
        }

        if (connectionContext.getCompressionThreshold() >= 0) {
            if (connectionContext.isEnabledCompressionForClient()) {
                var inBuffer = allocator.heapBuffer();
                var inPacketSize = PacketCompressionUtils.decompress(size, buffer, inBuffer);
                var outBuffer = allocator.heapBuffer();
                handle(allocator, inPacketSize, inBuffer, outBuffer);
                if (outBuffer.isReadable()) {
                    PacketCompressionUtils.compress(connectionContext.getCompressionThreshold(), PacketUtils.readVarint(outBuffer), outBuffer, out);
                }
                inBuffer.release();
                outBuffer.release();
            } else {
                if (packetType == PacketType.S2C) {
                    var inBuffer = allocator.heapBuffer();
                    var inPacketSize = PacketCompressionUtils.decompress(size, buffer, inBuffer);
                    handle(allocator, inPacketSize, inBuffer, out);
                    inBuffer.release();
                } else if (packetType == PacketType.C2S) {
                    var outBuffer = allocator.heapBuffer();
                    handle(allocator, size, buffer, outBuffer);
                    if (outBuffer.isReadable()) {
                        PacketCompressionUtils.compress(connectionContext.getCompressionThreshold(), PacketUtils.readVarint(outBuffer), outBuffer, out);
                    }
                    outBuffer.release();
                } else {
                    throw new AssertionError();
                }
            }
        } else {
            handle(allocator, size, buffer, out);
        }

        buffer.readerIndex(endIndex);
        return true;
    }

    private void handle(ByteBufAllocator allocator, int packetSize, ByteBuf in, ByteBuf out) throws Exception {
        var before = in.readerIndex();
        var id = PacketUtils.readVarint(in);
        var idLength = in.readerIndex() - before;
        var handler = packetType.getPacketHandler(connectionContext.getConnectionState(), id);

        if (handler == null) {
            PacketUtils.writeVarint(out, packetSize);
            PacketUtils.writeVarint(out, id);
            out.writeBytes(in, packetSize - idLength);
            return;
        }

        var start = in.readerIndex();
        var transformed = allocator.heapBuffer();
        var flag = handler.handle(connectionContext, in, transformed);

        if (flag == HandledFlag.PASSED) {
            in.readerIndex(start);
            PacketUtils.writeVarint(out, packetSize);
            PacketUtils.writeVarint(out, id);
            out.writeBytes(in, in.readerIndex(), packetSize - idLength);
        } else if (flag == HandledFlag.TRANSFORMED) {
            var size = idLength + transformed.readableBytes();
            PacketUtils.writeVarint(out, size);
            PacketUtils.writeVarint(out, id);
            out.writeBytes(transformed);
        }

        transformed.release();
    }
}

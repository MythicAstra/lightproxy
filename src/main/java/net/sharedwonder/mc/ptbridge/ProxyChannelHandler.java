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

package net.sharedwonder.mc.ptbridge;

import net.sharedwonder.mc.ptbridge.crypt.EncryptionContext;
import net.sharedwonder.mc.ptbridge.packet.HandledFlag;
import net.sharedwonder.mc.ptbridge.packet.PacketCompressionUtils;
import net.sharedwonder.mc.ptbridge.packet.PacketType;
import net.sharedwonder.mc.ptbridge.packet.PacketUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.jetbrains.annotations.NotNull;

public abstract sealed class ProxyChannelHandler extends ChannelInboundHandlerAdapter permits ProxyBackendHandler, ProxyServerHandler {
    protected final @NotNull ConnectionContext connectionContext;

    private final @NotNull PacketType packetType;

    private ByteBuf buffer;

    private int remainingUnreadSize = 0;

    protected ProxyChannelHandler(@NotNull ConnectionContext connectionContext, @NotNull PacketType packetType) {
        this.connectionContext = connectionContext;
        this.packetType = packetType;
    }

    protected abstract @NotNull ChannelFuture sendMessage(@NotNull ByteBuf message);

    @Override
    public void channelRead(@NotNull ChannelHandlerContext context, @NotNull Object message) {
        var in = (ByteBuf) message;
        try {
            if (!in.isReadable()) {
                return;
            }

            var encryptionContext = connectionContext.getEncryptionContext();
            var messageBuf = encryptionContext.isEnabled() ? encryptionContext.decrypt(packetType, in) : in;
            var allocator = context.alloc();

            if (remainingUnreadSize > 0) {
                var available = messageBuf.readableBytes();
                buffer.writeBytes(messageBuf);
                remainingUnreadSize -= available;

                if (remainingUnreadSize > 0) {
                    return;
                }
            } else {
                buffer = allocator.buffer();
                buffer.writeBytes(messageBuf);
            }

            var out = allocator.buffer();
            do {
                var startIndex = buffer.readerIndex();
                int size;
                try {
                    size = PacketUtils.readVarint(buffer);
                } catch (IndexOutOfBoundsException exception) {
                    remainingUnreadSize = 1;
                    buffer.readerIndex(startIndex);

                    send(encryptionContext, out);
                    return;
                }
                var endIndex = buffer.readerIndex() + size;
                var available = buffer.readableBytes();
                if (size > available) {
                    remainingUnreadSize = size - available;
                    buffer.readerIndex(startIndex);

                    send(encryptionContext, out);
                    return;
                }

                if (connectionContext.getCompressionThreshold() >= 0) {
                    if (packetType == PacketType.S2C) {
                        var buf = allocator.buffer();
                        var decompressedSize = PacketCompressionUtils.decompress(size, buffer, buf);
                        handle(allocator, decompressedSize, buf, out);
                        buf.release();
                    } else {
                        var packet = allocator.buffer();
                        handle(allocator, size, buffer, packet);
                        if (packet.isReadable()) {
                            PacketCompressionUtils.compress(connectionContext.getCompressionThreshold(), PacketUtils.readVarint(packet), packet, out);
                        }
                        packet.release();
                    }
                } else {
                    handle(allocator, size, buffer, out);
                }

                buffer.readerIndex(endIndex);
            } while (buffer.isReadable());

            if (packetType == PacketType.S2C) {
                while (!connectionContext.toClientPacketQueue.isEmpty()) {
                    out.writeBytes(connectionContext.toClientPacketQueue.poll());
                }
            } else {
                while (!connectionContext.toServerPacketQueue.isEmpty()) {
                    var packet = connectionContext.toServerPacketQueue.poll();
                    PacketCompressionUtils.compress(connectionContext.getCompressionThreshold(), PacketUtils.readVarint(packet), packet, out);
                    packet.release();
                }
            }

            send(encryptionContext, out);
            buffer.release();
        } finally {
            in.release();
        }
    }

    @Override
    public void channelInactive(@NotNull ChannelHandlerContext context) {
        if (buffer != null && buffer.refCnt() > 0) {
            remainingUnreadSize = 0;
            buffer.release();
        }
    }

    private void send(@NotNull EncryptionContext encryptionContext, @NotNull ByteBuf message) {
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

    private void handle(@NotNull ByteBufAllocator allocator, int packetSize, @NotNull ByteBuf in, @NotNull ByteBuf out) {
        var before = in.readerIndex();
        var id = PacketUtils.readVarint(in);
        var idWidth = in.readerIndex() - before;
        var handler = packetType.getPacketHandler(connectionContext.getConnectionState(), id);

        if (handler == null) {
            PacketUtils.writeVarint(out, packetSize);
            PacketUtils.writeVarint(out, id);
            out.writeBytes(in, packetSize - idWidth);
            return;
        }

        var start = in.readerIndex();
        var transformed = allocator.buffer();
        var flag = handler.handle(connectionContext, in, transformed);

        if (flag == HandledFlag.PASSED) {
            in.readerIndex(start);
            PacketUtils.writeVarint(out, packetSize);
            PacketUtils.writeVarint(out, id);
            out.writeBytes(in, in.readerIndex(), packetSize - idWidth);
        } else if (flag == HandledFlag.TRANSFORMED) {
            var size = idWidth + transformed.readableBytes();
            PacketUtils.writeVarint(out, size);
            PacketUtils.writeVarint(out, id);
            out.writeBytes(transformed);
        }

        transformed.release();
    }
}

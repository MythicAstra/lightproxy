/*
 * Copyright (C) 2025 MythicAstra
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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import net.sharedwonder.lightproxy.packet.PacketType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@ChannelHandler.Sharable
final class ProxyBackendHandler extends ProxyChannelHandler {
    private final Channel clientBoundChannel;

    ProxyBackendHandler(ConnectionContext connectionContext, Channel clientBoundChannel) {
        super(connectionContext, PacketType.S2C);
        this.clientBoundChannel = clientBoundChannel;
    }

    @Override
    public void channelInactive(ChannelHandlerContext context) {
        super.channelInactive(context);
        if (clientBoundChannel.isActive()) {
            clientBoundChannel.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext context, Throwable exception) {
        LOGGER.error("Caught an exception in the proxy backend handler", exception);
        context.close();
    }

    @Override
    ChannelFuture sendMessage(ByteBuf message) {
        return clientBoundChannel.writeAndFlush(message);
    }

    private static final Logger LOGGER = LogManager.getLogger(ProxyBackendHandler.class);
}

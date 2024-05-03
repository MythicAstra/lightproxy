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

import java.net.InetSocketAddress;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import net.sharedwonder.mc.ptbridge.packet.PacketType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

public final class ProxyServerHandler extends ProxyChannelHandler {
    private Channel serverBoundChannel;

    public ProxyServerHandler(@NotNull ProxyServer proxyServer) {
        super(new ConnectionContext(proxyServer), PacketType.C2S);
    }

    @Override
    public void channelActive(@NotNull ChannelHandlerContext context) {
        var channel = context.channel();
        channel.config().setAutoRead(false);

        connectionContext.setClientAddress(((InetSocketAddress) channel.remoteAddress()).getAddress().getHostAddress());

        var bootstrap = new Bootstrap();
        bootstrap.group(channel.eventLoop())
            .channel(channel.getClass())
            .option(ChannelOption.TCP_NODELAY, true)
            .handler(new ProxyBackendHandler(connectionContext, channel));

        var socketAddress = new InetSocketAddress(connectionContext.remoteAddress, connectionContext.remotePort);
        var channelFuture = bootstrap.connect(socketAddress);
        serverBoundChannel = channelFuture.channel().pipeline().context(ProxyBackendHandler.class).channel();

        connectionContext.onConnect();
        channelFuture.addListener((ChannelFutureListener) listener -> {
            if (listener.isSuccess()) {
                channel.config().setAutoRead(true);
            } else {
                LOGGER.error("Failed to connect to the remote host: {}", socketAddress);
                if (channel.isActive()) {
                    channel.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
                }
                connectionContext.onDisconnect();
            }
        });
    }

    @Override
    public void channelInactive(@NotNull ChannelHandlerContext context) {
        super.channelInactive(context);
        if (serverBoundChannel.isActive()) {
            sendMessage(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        }
        connectionContext.onDisconnect();
    }

    @Override
    public void exceptionCaught(@NotNull ChannelHandlerContext context, Throwable exception) {
        LOGGER.error("Caught an exception in the proxy server handler", exception);
        context.close();
    }

    @Override
    protected @NotNull ChannelFuture sendMessage(@NotNull ByteBuf message) {
        return serverBoundChannel.writeAndFlush(message);
    }

    private static final Logger LOGGER = LogManager.getLogger(ProxyServerHandler.class);
}

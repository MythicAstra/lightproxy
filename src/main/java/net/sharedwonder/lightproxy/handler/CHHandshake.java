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

package net.sharedwonder.lightproxy.handler;

import io.netty.buffer.ByteBuf;
import net.sharedwonder.lightproxy.ConnectionContext;
import net.sharedwonder.lightproxy.Constants;
import net.sharedwonder.lightproxy.packet.C2SPacketHandler;
import net.sharedwonder.lightproxy.packet.HandledFlag;
import net.sharedwonder.lightproxy.packet.PacketUtils;
import net.sharedwonder.lightproxy.util.ConnectionState;

public class CHHandshake implements C2SPacketHandler {
    @Override
    public int getId() {
        return Constants.PID_CH_HANDSHAKE;
    }

    @Override
    public HandledFlag handle(ConnectionContext context, ByteBuf in, ByteBuf transformed) {
        context.setProtocolVersion(PacketUtils.readVarint(in));
        PacketUtils.skipChunk(in);
        in.skipBytes(2);
        var requestedState = ConnectionState.getById(in.readByte());

        context.setConnectionState(requestedState);

        PacketUtils.writeVarint(transformed, context.getProtocolVersion());
        PacketUtils.writeUtf8String(transformed, context.getRemoteAddress());
        transformed.writeShort(context.getRemotePort());
        transformed.writeByte(requestedState.getId());

        return HandledFlag.TRANSFORMED;
    }
}

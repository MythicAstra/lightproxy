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

package net.sharedwonder.mc.ptbridge.handlers;

import net.sharedwonder.mc.ptbridge.ConnectionContext;
import net.sharedwonder.mc.ptbridge.packet.C2SPacketHandler;
import net.sharedwonder.mc.ptbridge.packet.HandledFlag;
import net.sharedwonder.mc.ptbridge.packet.PacketUtils;
import net.sharedwonder.mc.ptbridge.utils.ConnectionState;
import net.sharedwonder.mc.ptbridge.utils.Constants;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;

public class CHHandshake implements C2SPacketHandler {
    @Override
    public int getId() {
        return Constants.PID_CH_HANDSHAKE;
    }

    @Override
    public @NotNull HandledFlag handle(@NotNull ConnectionContext connectionContext, @NotNull ByteBuf in, @NotNull ByteBuf transformed) {
        connectionContext.setProtocolVersion(PacketUtils.readVarint(in));
        PacketUtils.skipChunk(in);
        in.skipBytes(2);
        var requestedState = ConnectionState.getById(in.readByte());

        connectionContext.setConnectionState(requestedState);

        PacketUtils.writeVarint(transformed, connectionContext.getProtocolVersion());
        PacketUtils.writeUtf8String(transformed, connectionContext.remoteAddress);
        transformed.writeShort(connectionContext.remotePort);
        transformed.writeByte(requestedState.getId());

        return HandledFlag.TRANSFORMED;
    }
}

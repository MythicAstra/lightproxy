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

package net.sharedwonder.lightproxy.handler;

import io.netty.buffer.ByteBuf;
import net.sharedwonder.lightproxy.ConnectionContext;
import net.sharedwonder.lightproxy.Constants;
import net.sharedwonder.lightproxy.packet.HandleFlag;
import net.sharedwonder.lightproxy.packet.PacketUtils;
import net.sharedwonder.lightproxy.packet.S2CPacketHandler;

public class SPV47SetCompressionLevel implements S2CPacketHandler {
    @Override
    public int getId() {
        return Constants.PID_SP_V47_SET_COMPRESSION_LEVEL;
    }

    @Override
    public HandleFlag handle(ConnectionContext context, ByteBuf in, ByteBuf transformed) {
        if (context.getProtocolVersion() == MINECRAFT_1_8_X_PROTOCOL_VERSION) {
            context.setCompressionThreshold(PacketUtils.readVarint(in));
            if (!context.isEnabledCompressionForClient()) {
                return HandleFlag.BLOCKED;
            }
        }
        return HandleFlag.PASSED;
    }

    private static final int MINECRAFT_1_8_X_PROTOCOL_VERSION = 47;
}

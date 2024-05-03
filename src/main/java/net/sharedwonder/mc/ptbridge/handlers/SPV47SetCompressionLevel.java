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

import io.netty.buffer.ByteBuf;
import net.sharedwonder.mc.ptbridge.ConnectionContext;
import net.sharedwonder.mc.ptbridge.packet.HandledFlag;
import net.sharedwonder.mc.ptbridge.packet.PacketUtils;
import net.sharedwonder.mc.ptbridge.packet.S2CPacketHandler;
import net.sharedwonder.mc.ptbridge.utils.Constants;
import org.jetbrains.annotations.NotNull;

public class SPV47SetCompressionLevel implements S2CPacketHandler {
    @Override
    public int getId() {
        return Constants.PID_SP_V47_SET_COMPRESSION_LEVEL;
    }

    @Override
    public @NotNull HandledFlag handle(@NotNull ConnectionContext connectionContext, @NotNull ByteBuf in, @NotNull ByteBuf transformed) {
        if (connectionContext.getProtocolVersion() == 47) {
            connectionContext.setCompressionThreshold(PacketUtils.readVarint(in));
            return HandledFlag.BLOCKED;
        }
        return HandledFlag.PASSED;
    }
}

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

import io.netty.buffer.ByteBuf;
import net.sharedwonder.mc.ptbridge.ConnectionContext;

public sealed interface PacketHandler permits C2SPacketHandler, S2CPacketHandler {
    int getId();

    HandledFlag handle(ConnectionContext context, ByteBuf in, ByteBuf transformed) throws Exception;
}

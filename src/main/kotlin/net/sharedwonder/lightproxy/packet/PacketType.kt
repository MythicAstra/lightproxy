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

package net.sharedwonder.lightproxy.packet

import net.sharedwonder.lightproxy.util.ConnectionState

enum class PacketType {
    C2S {
        override fun getPacketHandler(connectionState: ConnectionState, id: Int): C2SPacketHandler? {
            return when (connectionState) {
                ConnectionState.HANDSHAKE -> if (id != 0) null else PacketHandlers.getClientHandshakePacketHandler()
                ConnectionState.PLAY -> PacketHandlers.C2S_PLAY_PACKET_HANDLERS[id]
                ConnectionState.LOGIN -> PacketHandlers.C2S_LOGIN_PACKET_HANDLERS[id]
                ConnectionState.STATUS -> PacketHandlers.C2S_STATUS_PACKET_HANDLERS[id]
            }
        }
    },

    S2C {
        override fun getPacketHandler(connectionState: ConnectionState, id: Int): S2CPacketHandler? {
            return when (connectionState) {
                ConnectionState.HANDSHAKE -> null
                ConnectionState.PLAY -> PacketHandlers.S2C_PLAY_PACKET_HANDLERS[id]
                ConnectionState.LOGIN -> PacketHandlers.S2C_LOGIN_PACKET_HANDLERS[id]
                ConnectionState.STATUS -> PacketHandlers.S2C_STATUS_PACKET_HANDLERS[id]
            }
        }
    };

    abstract fun getPacketHandler(connectionState: ConnectionState, id: Int): PacketHandler?
}

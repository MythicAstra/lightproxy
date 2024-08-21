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

package net.sharedwonder.lightproxy.packet;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.sharedwonder.lightproxy.Constants;
import net.sharedwonder.lightproxy.handler.CHHandshake;
import net.sharedwonder.lightproxy.handler.CLEncryptionResponse;
import net.sharedwonder.lightproxy.handler.CLRequestLogin;
import net.sharedwonder.lightproxy.handler.SLEnableCompression;
import net.sharedwonder.lightproxy.handler.SLLoginSuccess;
import net.sharedwonder.lightproxy.handler.SLRequestEncryption;
import net.sharedwonder.lightproxy.handler.SPV47SetCompressionLevel;

public final class PacketHandlers {
    private PacketHandlers() {}

    public static final Map<Integer, C2SPacketHandler> C2S_PLAY_PACKET_HANDLERS = new HashMap<>();

    public static final Map<Integer, C2SPacketHandler> C2S_STATUS_PACKET_HANDLERS = new HashMap<>();

    public static final Map<Integer, C2SPacketHandler> C2S_LOGIN_PACKET_HANDLERS = new HashMap<>();

    public static final Map<Integer, S2CPacketHandler> S2C_PLAY_PACKET_HANDLERS = new HashMap<>();

    public static final Map<Integer, S2CPacketHandler> S2C_LOGIN_PACKET_HANDLERS = new HashMap<>();

    public static final Map<Integer, S2CPacketHandler> S2C_STATUS_PACKET_HANDLERS = new HashMap<>();

    @Nullable
    private static C2SPacketHandler clientHandshakePacketHandler;

    static {
        clientHandshakePacketHandler = new CHHandshake();

        registerHandler(C2S_LOGIN_PACKET_HANDLERS, new CLRequestLogin());
        registerHandler(C2S_LOGIN_PACKET_HANDLERS, new CLEncryptionResponse());

        registerHandler(S2C_LOGIN_PACKET_HANDLERS, new SLRequestEncryption());
        registerHandler(S2C_LOGIN_PACKET_HANDLERS, new SLLoginSuccess());
        registerHandler(S2C_LOGIN_PACKET_HANDLERS, new SLEnableCompression());

        registerHandler(S2C_PLAY_PACKET_HANDLERS, new SPV47SetCompressionLevel());
    }

    @Nullable
    public static C2SPacketHandler getClientHandshakePacketHandler() {
        return clientHandshakePacketHandler;
    }

    public static void setClientHandshakePacketHandler(@Nullable C2SPacketHandler handler) {
        if (handler != null && handler.getId() != Constants.PID_CH_HANDSHAKE) {
            throw new IllegalArgumentException("Handshake packet handler ID must be 0x0");
        }
        clientHandshakePacketHandler = handler;
    }

    public static <T extends PacketHandler> void registerHandler(Map<? super Integer, T> map, T handler) {
        map.put(handler.getId(), handler);
    }

    public static void unregisterHandler(Map<? super Integer, ? extends PacketHandler> map, int id) {
        map.remove(id);
    }
}

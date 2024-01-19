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

import net.sharedwonder.mc.ptbridge.handlers.CHHandshake;
import net.sharedwonder.mc.ptbridge.handlers.CLEncryptionResponse;
import net.sharedwonder.mc.ptbridge.handlers.CLRequestLogin;
import net.sharedwonder.mc.ptbridge.handlers.SLEnableCompression;
import net.sharedwonder.mc.ptbridge.handlers.SLLoginSuccess;
import net.sharedwonder.mc.ptbridge.handlers.SLRequestEncryption;
import net.sharedwonder.mc.ptbridge.handlers.SPV47SetCompressionLevel;
import net.sharedwonder.mc.ptbridge.utils.Constants;
import java.util.HashMap;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class PacketHandlers {
    private PacketHandlers() {}

    public static final Map<Integer, C2SPacketHandler> C2S_PLAY_PACKET_HANDLERS = new HashMap<>();

    public static final Map<Integer, C2SPacketHandler> C2S_STATUS_PACKET_HANDLERS = new HashMap<>();

    public static final Map<Integer, C2SPacketHandler> C2S_LOGIN_PACKET_HANDLERS = new HashMap<>();

    public static final Map<Integer, S2CPacketHandler> S2C_PLAY_PACKET_HANDLERS = new HashMap<>();

    public static final Map<Integer, S2CPacketHandler> S2C_LOGIN_PACKET_HANDLERS = new HashMap<>();

    public static final Map<Integer, S2CPacketHandler> S2C_STATUS_PACKET_HANDLERS = new HashMap<>();

    private static @Nullable C2SPacketHandler handshakePacketHandler;

    public static @Nullable C2SPacketHandler getHandshakePacketHandler() {
        return handshakePacketHandler;
    }

    public static void registerHandshakePacketHandler(@NotNull C2SPacketHandler handler) {
        if (handler.getId() != Constants.PID_CH_HANDSHAKE) {
            throw new IllegalArgumentException("Handshake packet handler ID must be 0x0");
        }
        handshakePacketHandler = handler;
    }

    public static void unregisterHandshakePacketHandler() {
        handshakePacketHandler = null;
    }

    public static <T extends PacketHandler> void registerHandler(@NotNull Map<? super Integer, T> map, @NotNull T handler) {
        map.put(handler.getId(), handler);
    }

    public static void unregisterHandler(@NotNull Map<? super Integer, ? extends PacketHandler> map, int id) {
        map.remove(id);
    }

    public static void registerDefaultHandlers() {
        registerHandshakePacketHandler(new CHHandshake());

        registerHandler(C2S_LOGIN_PACKET_HANDLERS, new CLRequestLogin());
        registerHandler(C2S_LOGIN_PACKET_HANDLERS, new CLEncryptionResponse());

        registerHandler(S2C_LOGIN_PACKET_HANDLERS, new SLRequestEncryption());
        registerHandler(S2C_LOGIN_PACKET_HANDLERS, new SLLoginSuccess());
        registerHandler(S2C_LOGIN_PACKET_HANDLERS, new SLEnableCompression());

        registerHandler(S2C_PLAY_PACKET_HANDLERS, new SPV47SetCompressionLevel());
    }
}

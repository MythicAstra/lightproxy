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
import net.sharedwonder.mc.ptbridge.Constants;
import net.sharedwonder.mc.ptbridge.crypt.CryptUtils;
import net.sharedwonder.mc.ptbridge.crypt.EncryptionContext;
import net.sharedwonder.mc.ptbridge.packet.HandledFlag;
import net.sharedwonder.mc.ptbridge.packet.PacketUtils;
import net.sharedwonder.mc.ptbridge.packet.S2CPacketHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SLRequestEncryption implements S2CPacketHandler {
    @Override
    public int getId() {
        return Constants.PID_SL_REQUEST_ENCRYPTION;
    }

    @Override
    public HandledFlag handle(ConnectionContext context, ByteBuf in, ByteBuf transformed) {
        var baseServerId = PacketUtils.readUtf8String(in);
        var publicKey = CryptUtils.decodePublicKey(PacketUtils.readByteArray(in));
        var verifyToken = PacketUtils.readByteArray(in);

        var handshakingContext = EncryptionContext.handshaking(baseServerId, publicKey, verifyToken);
        context.setEncryptionContext(handshakingContext);

        PacketUtils.writeUtf8String(transformed, baseServerId);
        PacketUtils.writeByteArray(transformed, handshakingContext.proxyServerPublicKey.getEncoded());
        PacketUtils.writeByteArray(transformed, verifyToken);

        LOGGER.info("Server requested encryption, client username: " + context.getPlayerUsername());

        return HandledFlag.TRANSFORMED;
    }

    private static final Logger LOGGER = LogManager.getLogger(SLRequestEncryption.class);
}

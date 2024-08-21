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
import net.sharedwonder.lightproxy.crypt.CryptUtils;
import net.sharedwonder.lightproxy.crypt.EncryptionHandshakingContext;
import net.sharedwonder.lightproxy.packet.HandledFlag;
import net.sharedwonder.lightproxy.packet.PacketUtils;
import net.sharedwonder.lightproxy.packet.S2CPacketHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SLRequestEncryption implements S2CPacketHandler {
    @Override
    public int getId() {
        return Constants.PID_SL_REQUEST_ENCRYPTION;
    }

    @Override
    public HandledFlag handle(ConnectionContext context, ByteBuf in, ByteBuf transformed) {
        LOGGER.info(() -> "Server requested encryption, client username: " + context.getPlayerUsername());

        var baseServerId = PacketUtils.readUtf8String(in);
        var publicKey = CryptUtils.decodePublicKey(PacketUtils.readByteArray(in));
        var verifyToken = PacketUtils.readByteArray(in);

        var handshakingContext = new EncryptionHandshakingContext(baseServerId, publicKey, verifyToken);
        context.setEncryptionContext(handshakingContext);

        PacketUtils.writeUtf8String(transformed, baseServerId);
        PacketUtils.writeByteArray(transformed, handshakingContext.proxyServerPublicKey.getEncoded());
        PacketUtils.writeByteArray(transformed, verifyToken);

        return HandledFlag.TRANSFORMED;
    }

    private static final Logger LOGGER = LogManager.getLogger(SLRequestEncryption.class);
}

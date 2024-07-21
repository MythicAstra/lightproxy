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

import java.util.Arrays;
import io.netty.buffer.ByteBuf;
import net.sharedwonder.mc.ptbridge.ConnectionContext;
import net.sharedwonder.mc.ptbridge.Constants;
import net.sharedwonder.mc.ptbridge.crypt.CryptUtils;
import net.sharedwonder.mc.ptbridge.crypt.EncryptionContext;
import net.sharedwonder.mc.ptbridge.crypt.EncryptionHandshakingContext;
import net.sharedwonder.mc.ptbridge.packet.C2SPacketHandler;
import net.sharedwonder.mc.ptbridge.packet.HandledFlag;
import net.sharedwonder.mc.ptbridge.packet.PacketUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CLEncryptionResponse implements C2SPacketHandler {
    @Override
    public int getId() {
        return Constants.PID_CL_ENCRYPTION_RESPONSE;
    }

    @Override
    public HandledFlag handle(ConnectionContext context, ByteBuf in, ByteBuf transformed) {
        if (!(context.getEncryptionContext() instanceof EncryptionHandshakingContext handshakingContext)) {
            throw new IllegalStateException("Encryption context is not handshaking context");
        }

        var username = context.getPlayerUsername();
        if (context.accounts == null || context.accounts.isEmpty()) {
            throw new RuntimeException("Unable to enable encryption because no Minecraft accounts were configured: " + username);
        }

        var proxyServerSecretKey = CryptUtils.generateSecretKey();
        var clientSecretKey = CryptUtils.decodeSecretKey(CryptUtils.decryptData(handshakingContext.proxyServerPrivateKey, PacketUtils.readByteArray(in)));
        var verifyToken = CryptUtils.decryptData(handshakingContext.proxyServerPrivateKey, PacketUtils.readByteArray(in));

        var profile = context.accounts.get(username);
        if (profile != null) {
            if (!profile.hasJoinedServer(CryptUtils.calcServerId(handshakingContext.baseServerId, clientSecretKey, handshakingContext.proxyServerPublicKey),
                context.getClientAddress())) {
                throw new RuntimeException("Unable to authenticate the client (hasJoinedServer check): " + username);
            }
            if (!Arrays.equals(verifyToken, handshakingContext.verifyToken)) {
                throw new RuntimeException("Unable to authenticate the client (verifyToken check): " + username);
            }

            profile.joinServer(CryptUtils.calcServerId(handshakingContext.baseServerId, proxyServerSecretKey, handshakingContext.originServerPublicKey));
        } else {
            throw new RuntimeException("Unable to enable encryption because the Minecraft account profile of the username was not found: " + username);
        }

        context.setEncryptionContext(EncryptionContext.enabled(clientSecretKey, proxyServerSecretKey));

        PacketUtils.writeByteArray(transformed, CryptUtils.encryptData(handshakingContext.originServerPublicKey, proxyServerSecretKey.getEncoded()));
        PacketUtils.writeByteArray(transformed, CryptUtils.encryptData(handshakingContext.originServerPublicKey, handshakingContext.verifyToken));

        LOGGER.info("Client authenticated, username: " + context.getPlayerUsername());

        return HandledFlag.TRANSFORMED;
    }

    private static final Logger LOGGER = LogManager.getLogger(CLEncryptionResponse.class);
}

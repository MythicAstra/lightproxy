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

import java.security.MessageDigest;
import java.security.PublicKey;
import java.util.Arrays;
import javax.crypto.SecretKey;
import io.netty.buffer.ByteBuf;
import net.sharedwonder.lightproxy.ConnectionContext;
import net.sharedwonder.lightproxy.Constants;
import net.sharedwonder.lightproxy.crypt.CryptUtils;
import net.sharedwonder.lightproxy.crypt.EncryptionEnabledContext;
import net.sharedwonder.lightproxy.crypt.EncryptionHandshakingContext;
import net.sharedwonder.lightproxy.packet.C2SPacketHandler;
import net.sharedwonder.lightproxy.packet.HandleFlag;
import net.sharedwonder.lightproxy.packet.PacketUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CLEncryptionResponse implements C2SPacketHandler {
    @Override
    public int getId() {
        return Constants.PID_CL_ENCRYPTION_RESPONSE;
    }

    @Override
    public HandleFlag handle(ConnectionContext context, ByteBuf in, ByteBuf transformed) {
        if (!(context.getEncryptionContext() instanceof EncryptionHandshakingContext handshakingContext)) {
            throw new IllegalStateException("Encryption context is not handshaking context");
        }

        var username = context.getPlayerUsername();
        var accounts = context.getAccounts();
        if (accounts == null || accounts.isEmpty()) {
            throw new RuntimeException("Unable to enable encryption because no Minecraft accounts were configured: " + username);
        }

        var secretKey = CryptUtils.decodeSecretKey(CryptUtils.decryptData(handshakingContext.proxyServerPrivateKey, PacketUtils.readByteArray(in)));
        var verifyToken = CryptUtils.decryptData(handshakingContext.proxyServerPrivateKey, PacketUtils.readByteArray(in));

        var profile = accounts.get(username);
        if (profile != null) {
            if (!context.isClientFromLocalhost()) {
                if (!profile.hasJoinedServer(calcServerId(handshakingContext.baseServerId, secretKey, handshakingContext.proxyServerPublicKey), context.getClientAddress())) {
                    throw new RuntimeException("Unable to authenticate the client (hasJoinedServer check): " + username);
                }

                LOGGER.info(() -> "Client authenticated, username: " + context.getPlayerUsername());
            }

            LOGGER.info(() -> "Client from localhost, not authenticating, username: " + context.getPlayerUsername());

            if (!Arrays.equals(verifyToken, handshakingContext.verifyToken)) {
                throw new RuntimeException("Unable to authenticate the client (verifyToken check): " + username);
            }

            profile.joinServer(calcServerId(handshakingContext.baseServerId, secretKey, handshakingContext.originServerPublicKey));
        } else {
            throw new RuntimeException("Unable to enable encryption because the Minecraft account profile of the username was not found: " + username);
        }

        context.setEncryptionContext(new EncryptionEnabledContext(secretKey));

        PacketUtils.writeByteArray(transformed, CryptUtils.encryptData(handshakingContext.originServerPublicKey, secretKey.getEncoded()));
        PacketUtils.writeByteArray(transformed, CryptUtils.encryptData(handshakingContext.originServerPublicKey, handshakingContext.verifyToken));

        return HandleFlag.TRANSFORMED;
    }

    private static final Logger LOGGER = LogManager.getLogger(CLEncryptionResponse.class);

    private static byte[] calcServerId(byte[] baseServerId, SecretKey secretKey, PublicKey publicKey) {
        try {
            var messageDigest = MessageDigest.getInstance("SHA-1");
            messageDigest.update(baseServerId);
            messageDigest.update(secretKey.getEncoded());
            messageDigest.update(publicKey.getEncoded());
            return messageDigest.digest();
        } catch (Exception exception) {
            throw new RuntimeException("Failed to calculate server id", exception);
        }
    }
}

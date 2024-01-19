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

package net.sharedwonder.mc.ptbridge.crypt;

import net.sharedwonder.mc.ptbridge.packet.PacketType;
import java.security.PublicKey;
import javax.crypto.SecretKey;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;

public sealed interface EncryptionContext permits EncryptionEnabledContext, EncryptionDisabledContext, EncryptionHandshakingContext {
    default @NotNull ByteBuf encrypt(@NotNull PacketType type, @NotNull ByteBuf in) {
        throw new UnsupportedOperationException();
    }

    default @NotNull ByteBuf decrypt(@NotNull PacketType type, @NotNull ByteBuf in) {
        throw new UnsupportedOperationException();
    }

    default boolean isEnabled() {
        return false;
    }

    static @NotNull EncryptionDisabledContext disabled() {
        return new EncryptionDisabledContext();
    }

    static @NotNull EncryptionHandshakingContext handshaking(@NotNull String baseServerId, @NotNull PublicKey originServerPublicKey, byte @NotNull [] verifyToken) {
        return new EncryptionHandshakingContext(baseServerId, originServerPublicKey, verifyToken);
    }

    static @NotNull EncryptionEnabledContext enabled(@NotNull SecretKey clientSecretKey, @NotNull SecretKey proxyServerSecretKey) {
        return new EncryptionEnabledContext(clientSecretKey, proxyServerSecretKey);
    }
}

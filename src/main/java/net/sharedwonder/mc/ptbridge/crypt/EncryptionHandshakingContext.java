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

import java.security.PrivateKey;
import java.security.PublicKey;
import org.jetbrains.annotations.NotNull;

public final class EncryptionHandshakingContext implements EncryptionContext {
    public final @NotNull String baseServerId;

    public final @NotNull PublicKey originServerPublicKey;

    public final byte @NotNull [] verifyToken;

    public final @NotNull PublicKey proxyServerPublicKey;

    public final @NotNull PrivateKey proxyServerPrivateKey;

    EncryptionHandshakingContext(@NotNull String baseServerId, @NotNull PublicKey originServerPublicKey, byte @NotNull [] verifyToken) {
        this.baseServerId = baseServerId;
        this.originServerPublicKey = originServerPublicKey;
        this.verifyToken = verifyToken;
        var keyPair = CryptUtils.generateKeyPair();
        proxyServerPublicKey = keyPair.getPublic();
        proxyServerPrivateKey = keyPair.getPrivate();
    }
}

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

package net.sharedwonder.lightproxy.crypt;

import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.PublicKey;

public final class EncryptionHandshakingContext implements EncryptionContext {
    public final byte[] baseServerId;

    public final PublicKey originServerPublicKey;

    public final byte[] verifyToken;

    public final PublicKey proxyServerPublicKey;

    public final PrivateKey proxyServerPrivateKey;

    public EncryptionHandshakingContext(String baseServerId, PublicKey originServerPublicKey, byte[] verifyToken) {
        this.baseServerId = baseServerId.getBytes(StandardCharsets.ISO_8859_1);
        this.originServerPublicKey = originServerPublicKey;
        this.verifyToken = verifyToken;
        var keyPair = CryptUtils.generateKeyPair();
        proxyServerPublicKey = keyPair.getPublic();
        proxyServerPrivateKey = keyPair.getPrivate();
    }
}

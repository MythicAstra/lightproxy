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

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.jetbrains.annotations.NotNull;

public final class CryptUtils {
    private CryptUtils() {}

    public static @NotNull SecretKey generateSecretKey() {
        try {
            var generator = KeyGenerator.getInstance("AES");
            generator.init(128);
            return generator.generateKey();
        } catch (Exception exception) {
            throw new RuntimeException("Failed to generate secret key", exception);
        }
    }

    public static @NotNull KeyPair generateKeyPair() {
        try {
            var generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(1024);
            return generator.generateKeyPair();
        } catch (Exception exception) {
            throw new RuntimeException("Failed to generate key pair", exception);
        }
    }

    public static @NotNull PublicKey decodePublicKey(byte @NotNull [] encoded) {
        try {
            var encodedKeySpec = new X509EncodedKeySpec(encoded);
            var keyfactory = KeyFactory.getInstance("RSA");
            return keyfactory.generatePublic(encodedKeySpec);
        } catch (Exception exception) {
            throw new RuntimeException("Failed to decode public key", exception);
        }
    }

    public static @NotNull SecretKey decodeSecretKey(byte @NotNull [] encoded) {
        return new SecretKeySpec(encoded, "AES");
    }

    public static byte @NotNull [] encryptData(@NotNull Key key, byte @NotNull [] data) {
        try {
            var cipher = Cipher.getInstance(key.getAlgorithm());
            cipher.init(Cipher.ENCRYPT_MODE, key);
            return cipher.doFinal(data);
        } catch (Exception exception) {
            throw new RuntimeException("Failed to encrypt data", exception);
        }
    }

    public static byte @NotNull [] decryptData(@NotNull Key key, byte @NotNull [] data) {
        try {
            var cipher = Cipher.getInstance(key.getAlgorithm());
            cipher.init(Cipher.DECRYPT_MODE, key);
            return cipher.doFinal(data);
        } catch (Exception exception) {
            throw new RuntimeException("Failed to decrypt data", exception);
        }
    }

    public static byte @NotNull [] calcServerId(@NotNull String baseServerId, @NotNull SecretKey secretKey, @NotNull PublicKey publicKey) {
        try {
            var messageDigest = MessageDigest.getInstance("SHA-1");
            messageDigest.update(baseServerId.getBytes(StandardCharsets.ISO_8859_1));
            messageDigest.update(secretKey.getEncoded());
            messageDigest.update(publicKey.getEncoded());
            return messageDigest.digest();
        } catch (Exception exception) {
            throw new RuntimeException("Failed to calculate server id hash", exception);
        }
    }
}

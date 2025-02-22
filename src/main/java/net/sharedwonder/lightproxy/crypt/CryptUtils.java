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

package net.sharedwonder.lightproxy.crypt;

import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public final class CryptUtils {
    private CryptUtils() {}

    private static final String KEY_PAIR_ALGORITHM = "RSA";

    private static final String SECRET_KEY_ALGORITHM = "AES";

    private static final int KEY_PAIR_KEY_SIZE = 1024;

    public static KeyPair generateKeyPair() {
        try {
            var generator = KeyPairGenerator.getInstance(KEY_PAIR_ALGORITHM);
            generator.initialize(KEY_PAIR_KEY_SIZE);
            return generator.generateKeyPair();
        } catch (Exception exception) {
            throw new RuntimeException("Failed to generate key pair", exception);
        }
    }

    public static PublicKey decodePublicKey(byte[] encoded) {
        try {
            var encodedKeySpec = new X509EncodedKeySpec(encoded);
            var keyfactory = KeyFactory.getInstance(KEY_PAIR_ALGORITHM);
            return keyfactory.generatePublic(encodedKeySpec);
        } catch (Exception exception) {
            throw new RuntimeException("Failed to decode public key", exception);
        }
    }

    public static SecretKey decodeSecretKey(byte[] encoded) {
        return new SecretKeySpec(encoded, SECRET_KEY_ALGORITHM);
    }

    public static byte[] encryptData(Key key, byte[] data) {
        try {
            var cipher = Cipher.getInstance(key.getAlgorithm());
            cipher.init(Cipher.ENCRYPT_MODE, key);
            return cipher.doFinal(data);
        } catch (Exception exception) {
            throw new RuntimeException("Failed to encrypt data", exception);
        }
    }

    public static byte[] decryptData(Key key, byte[] data) {
        try {
            var cipher = Cipher.getInstance(key.getAlgorithm());
            cipher.init(Cipher.DECRYPT_MODE, key);
            return cipher.doFinal(data);
        } catch (Exception exception) {
            throw new RuntimeException("Failed to decrypt data", exception);
        }
    }
}

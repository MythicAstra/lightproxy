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

import java.security.GeneralSecurityException;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.IvParameterSpec;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.sharedwonder.lightproxy.packet.PacketType;
import net.sharedwonder.lightproxy.packet.PacketUtils;

public final class EncryptionEnabledContext implements EncryptionContext {
    private final Cipher c2sEncryptionCipher;

    private final Cipher c2sDecryptionCipher;

    private final Cipher s2cEncryptionCipher;

    private final Cipher s2cDecryptionCipher;

    public EncryptionEnabledContext(SecretKey secretKey) {
        try {
            c2sEncryptionCipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            c2sEncryptionCipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(secretKey.getEncoded()));
            c2sDecryptionCipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            c2sDecryptionCipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(secretKey.getEncoded()));

            s2cEncryptionCipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            s2cEncryptionCipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(secretKey.getEncoded()));
            s2cDecryptionCipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            s2cDecryptionCipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(secretKey.getEncoded()));
        } catch (GeneralSecurityException exception) {
            throw new RuntimeException(exception);
        }
    }

    @Override
    public ByteBuf encrypt(PacketType type, ByteBuf in) {
        return operate(in, type == PacketType.C2S ? c2sEncryptionCipher : s2cEncryptionCipher);
    }

    @Override
    public ByteBuf decrypt(PacketType type, ByteBuf in) {
        return operate(in, type == PacketType.C2S ? c2sDecryptionCipher : s2cDecryptionCipher);
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    private static final String CIPHER_TRANSFORMATION = "AES/CFB8/NoPadding";

    private static ByteBuf operate(ByteBuf in, Cipher cipher) {
        var size = cipher.getOutputSize(in.readableBytes());
        var bytes = PacketUtils.readBytes(in);
        try {
            var buffer = new byte[size];
            return Unpooled.wrappedBuffer(buffer, 0, cipher.update(bytes, 0, bytes.length, buffer));
        } catch (ShortBufferException exception) {
            throw new RuntimeException(exception);
        }
    }
}

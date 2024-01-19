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
import net.sharedwonder.mc.ptbridge.packet.PacketUtils;
import java.security.GeneralSecurityException;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.IvParameterSpec;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.jetbrains.annotations.NotNull;

public final class EncryptionEnabledContext implements EncryptionContext {
    private final @NotNull Cipher c2sEncryptionCipher;

    private final @NotNull Cipher c2sDecryptionCipher;

    private final @NotNull Cipher s2cEncryptionCipher;

    private final @NotNull Cipher s2cDecryptionCipher;

    EncryptionEnabledContext(@NotNull SecretKey clientSecretKey, @NotNull SecretKey proxyServerSecretKey) {
        try {
            var transformation = "AES/CFB8/NoPadding";

            c2sEncryptionCipher = Cipher.getInstance(transformation);
            c2sEncryptionCipher.init(Cipher.ENCRYPT_MODE, proxyServerSecretKey, new IvParameterSpec(proxyServerSecretKey.getEncoded()));

            c2sDecryptionCipher = Cipher.getInstance(transformation);
            c2sDecryptionCipher.init(Cipher.DECRYPT_MODE, clientSecretKey, new IvParameterSpec(clientSecretKey.getEncoded()));

            s2cEncryptionCipher = Cipher.getInstance(transformation);
            s2cEncryptionCipher.init(Cipher.ENCRYPT_MODE, clientSecretKey, new IvParameterSpec(clientSecretKey.getEncoded()));

            s2cDecryptionCipher = Cipher.getInstance(transformation);
            s2cDecryptionCipher.init(Cipher.DECRYPT_MODE, proxyServerSecretKey, new IvParameterSpec(proxyServerSecretKey.getEncoded()));
        } catch (GeneralSecurityException exception) {
            throw new RuntimeException(exception);
        }
    }

    @Override
    public @NotNull ByteBuf encrypt(@NotNull PacketType type, @NotNull ByteBuf in) {
        return operate(in, type == PacketType.C2S ? c2sEncryptionCipher : s2cEncryptionCipher);
    }

    @Override
    public @NotNull ByteBuf decrypt(@NotNull PacketType type, @NotNull ByteBuf in) {
        return operate(in, type == PacketType.C2S ? c2sDecryptionCipher : s2cDecryptionCipher);
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    private static ByteBuf operate(@NotNull ByteBuf in, @NotNull Cipher cipher) {
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

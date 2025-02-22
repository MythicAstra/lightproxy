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

import io.netty.buffer.ByteBuf;
import net.sharedwonder.lightproxy.packet.PacketType;

public sealed interface EncryptionContext permits EncryptionEnabledContext, EncryptionDisabledContext, EncryptionHandshakingContext {
    default ByteBuf encrypt(PacketType type, ByteBuf in) {
        throw new UnsupportedOperationException();
    }

    default ByteBuf decrypt(PacketType type, ByteBuf in) {
        throw new UnsupportedOperationException();
    }

    default boolean isEnabled() {
        return false;
    }
}

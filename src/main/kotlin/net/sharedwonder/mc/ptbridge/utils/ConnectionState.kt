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

package net.sharedwonder.mc.ptbridge.utils

enum class ConnectionState(val id: Int) {
    HANDSHAKE(-1),

    PLAY(0),

    STATUS(1),

    LOGIN(2);

    companion object {
        @JvmStatic
        fun getById(id: Int): ConnectionState {
            return when (id) {
                -1 -> HANDSHAKE
                0 -> PLAY
                1 -> STATUS
                2 -> LOGIN
                else -> throw IllegalArgumentException("Invalid id: $id")
            }
        }
    }
}

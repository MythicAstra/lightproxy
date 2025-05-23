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

package net.sharedwonder.lightproxy.util

import java.util.UUID

object UuidUtils {
    @JvmStatic
    @OptIn(ExperimentalStdlibApi::class)
    fun stringToUuid(uuid: String): UUID = UUID(uuid.take(16).hexToLong(), uuid.drop(16).hexToLong())

    @JvmStatic
    @OptIn(ExperimentalStdlibApi::class)
    fun uuidToString(uuid: UUID): String = "${uuid.mostSignificantBits.toHexString()}${uuid.leastSignificantBits.toHexString()}"
}

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

import java.io.File
import java.io.IOException
import com.google.gson.reflect.TypeToken

object AccountsFileUtils {
    fun readFile(file: File): MutableMap<String, PlayerProfile> {
        return if (file.isFile) {
            file.reader().use {
                @Suppress("UNCHECKED_CAST")
                GSON.fromJson(it, TypeToken.getParameterized(Map::class.java, String::class.java, PlayerProfile::class.java)) as MutableMap<String, PlayerProfile>
            }
        } else mutableMapOf()
    }

    @Throws(IOException::class)
    fun writeFile(file: File, accounts: Map<String, PlayerProfile>) {
        val jsonWriter = GSON.newJsonWriter(file.writer())
        jsonWriter.setIndent("    ")
        jsonWriter.use { GSON.toJson(accounts, Map::class.java, it) }
    }
}

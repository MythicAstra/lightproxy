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

package net.sharedwonder.lightproxy.util

import java.io.Reader
import java.io.StringReader
import com.google.gson.stream.JsonReader

open class JsonParser(val reader: Reader) : JsonReader(reader) {
    constructor(string: String) : this(StringReader(string))

    fun <R> nextObject(block: JsonParser.() -> R): R {
        beginObject()
        val result = block()
        endObject()
        return result
    }

    fun <R> nextArray(block: JsonParser.() -> R): R {
        beginArray()
        val result = block()
        endArray()
        return result
    }
}

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
import java.lang.reflect.Type
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter

object JsonUtils {
    private val gson = Gson()

    @JvmStatic
    fun <T> fromJson(json: String?, type: Class<T>): T = gson.fromJson(json, type)

    @JvmStatic
    fun <T> fromJson(json: String?, type: Type): T = gson.fromJson(json, type)

    @JvmStatic
    fun <T> fromJson(json: String?, type: TypeToken<T>): T = gson.fromJson(json, type)

    @JvmStatic
    fun <T> fromJson(json: Reader, type: Class<T>): T = gson.fromJson(json, type)

    @JvmStatic
    fun <T> fromJson(json: Reader, type: Type): T = gson.fromJson(json, type)

    @JvmStatic
    fun <T> fromJson(json: Reader, type: TypeToken<T>): T = gson.fromJson(json, type)

    @JvmStatic
    fun <T> fromJson(json: JsonReader, type: Class<T>): T = gson.fromJson(json, type)

    @JvmStatic
    fun <T> fromJson(json: JsonReader, type: Type): T = gson.fromJson(json, type)

    @JvmStatic
    fun <T> fromJson(json: JsonReader, type: TypeToken<T>): T = gson.fromJson(json, type)

    @JvmStatic
    fun <T> fromJson(json: JsonElement?, type: Class<T>): T = gson.fromJson(json, type)

    @JvmStatic
    fun <T> fromJson(json: JsonElement?, type: Type): T = gson.fromJson(json, type)

    @JvmStatic
    fun <T> fromJson(json: JsonElement?, type: TypeToken<T>): T = gson.fromJson(json, type)

    @JvmStatic
    inline fun <reified T> fromJson(json: String?): T = fromJson(json, T::class.java)

    @JvmStatic
    inline fun <reified T> fromJson(json: Reader): T = fromJson(json, T::class.java)

    @JvmStatic
    inline fun <reified T> fromJson(json: JsonReader): T = fromJson(json, T::class.java)

    @JvmStatic
    inline fun <reified T> fromJson(json: JsonElement?): T = fromJson(json, T::class.java)

    @JvmStatic
    fun toJson(src: Any): String = gson.toJson(src)

    @JvmStatic
    fun toJson(src: Any, type: Type): String = gson.toJson(src, type)

    @JvmStatic
    fun toJson(src: Any, appendable: Appendable) = gson.toJson(src, appendable)

    @JvmStatic
    fun toJson(src: Any, type: Type, appendable: Appendable) = gson.toJson(src, type, appendable)

    @JvmStatic
    fun toJson(src: Any, type: Type, writer: JsonWriter) = gson.toJson(src, type, writer)

    @JvmStatic
    fun toJson(jsonElement: JsonElement): String = gson.toJson(jsonElement)

    @JvmStatic
    fun toJson(jsonElement: JsonElement, appendable: Appendable) = gson.toJson(jsonElement, appendable)

    @JvmStatic
    fun toJson(jsonElement: JsonElement, writer: JsonWriter) = gson.toJson(jsonElement, writer)

    @JvmStatic
    fun toJsonTree(src: Any): JsonElement = gson.toJsonTree(src)

    @JvmStatic
    fun toJsonTree(src: Any, type: Type): JsonElement = gson.toJsonTree(src, type)
}

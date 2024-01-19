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

import com.google.gson.JsonObject

object TextUtils {
    private val FORMATTING_REGEX = Regex("§[0-9a-fk-or]", RegexOption.IGNORE_CASE)

    @JvmStatic
    fun serialize(text: String): String {
        val obj = JsonObject()
        obj.addProperty("text", text)
        return GSON.toJson(obj)
    }

    @JvmStatic
    fun colorOfId(colorId: Byte): String {
        return when (colorId.toInt()) {
            0 -> FormattedText.BLACK
            1 -> FormattedText.DARK_BLUE
            2 -> FormattedText.DARK_GREEN
            3 -> FormattedText.DARK_AQUA
            4 -> FormattedText.DARK_RED
            5 -> FormattedText.DARK_PURPLE
            6 -> FormattedText.GOLD
            7 -> FormattedText.GRAY
            8 -> FormattedText.DARK_GRAY
            9 -> FormattedText.BLUE
            10 -> FormattedText.GREEN
            11 -> FormattedText.AQUA
            12 -> FormattedText.RED
            13 -> FormattedText.LIGHT_PURPLE
            14 -> FormattedText.YELLOW
            15 -> FormattedText.WHITE
            else -> throw IllegalArgumentException("Invalid color: $colorId")
        }
    }

    @JvmStatic
    fun toPlaintext(text: String): String = text.replace(FORMATTING_REGEX, "")

    @JvmStatic
    fun alignTextTable(table: List<Array<String>>, separator: String): Array<String> {
        val aligned = arrayOfNulls<String>(table.size)

        val maxColumnLengths = ArrayList<Int>()
        for (row in table) {
            for ((index, text) in row.withIndex()) {
                val length = toPlaintext(text).length
                if (index >= maxColumnLengths.size) {
                    maxColumnLengths.add(length)
                } else {
                    maxColumnLengths[index] = maxOf(maxColumnLengths[index], length)
                }
            }
        }

        for ((rowIndex, row) in table.withIndex()) {
            val builder = StringBuilder()
            for ((index, text) in row.withIndex()) {
                builder.append(FormattedText.RESET).append(text).append(" ".repeat(maxColumnLengths[index] - toPlaintext(text).length))
                if (index < row.lastIndex) {
                    builder.append(separator)
                }
            }
            aligned[rowIndex] = builder.toString()
        }

        @Suppress("UNCHECKED_CAST")
        return aligned as Array<String>
    }
}

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

import java.io.IOException
import java.io.StringWriter
import com.google.gson.stream.JsonWriter

object MCText {
    const val BLACK: String = "§0"
    const val DARK_BLUE: String = "§1"
    const val DARK_GREEN: String = "§2"
    const val DARK_AQUA: String = "§3"
    const val DARK_RED: String = "§4"
    const val DARK_PURPLE: String = "§5"
    const val GOLD: String = "§6"
    const val GRAY: String = "§7"
    const val DARK_GRAY: String = "§8"
    const val BLUE: String = "§9"
    const val GREEN: String = "§a"
    const val AQUA: String = "§b"
    const val RED: String = "§c"
    const val LIGHT_PURPLE: String = "§d"
    const val YELLOW: String = "§e"
    const val WHITE: String = "§f"

    const val OBFUSCATED: String = "§k"
    const val BOLD: String = "§l"
    const val STRIKETHROUGH: String = "§m"
    const val UNDERLINE: String = "§n"
    const val ITALIC: String = "§o"

    const val RESET: String = "§r"

    private val validEscapeChars: CharArray = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f', 'k', 'l', 'm', 'n', 'o', 'r')

    @JvmStatic
    fun serialize(text: String): String {
        try {
            return StringWriter().use {
                JsonWriter(it).beginObject().name("text").value(text).endObject()
                it.toString()
            }
        } catch (exception: IOException) {
            // StringWriter never throws IOException
            throw AssertionError()
        }
    }

    @JvmStatic
    fun getColor(colorId: Byte): String {
        return when (colorId.toInt()) {
            0 -> BLACK
            1 -> DARK_BLUE
            2 -> DARK_GREEN
            3 -> DARK_AQUA
            4 -> DARK_RED
            5 -> DARK_PURPLE
            6 -> GOLD
            7 -> GRAY
            8 -> DARK_GRAY
            9 -> BLUE
            10 -> GREEN
            11 -> AQUA
            12 -> RED
            13 -> LIGHT_PURPLE
            14 -> YELLOW
            15 -> WHITE
            else -> throw IllegalArgumentException("Invalid color id: $colorId")
        }
    }

    @JvmStatic
    fun toPlaintext(text: String): String {
        val builder = StringBuilder()
        var escape = false
        for (char in text) {
            when {
                escape -> {
                    escape = false
                    if (char !in validEscapeChars) {
                        builder.append(char)
                    }
                }
                char == '§' -> escape = true
                else -> builder.append(char)
            }
        }
        return builder.toString()
    }

    @JvmStatic
    fun isFormattedText(text: String): Boolean = text.length == 2 && text[0] == '§' && text[1] in validEscapeChars

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
                builder.append(RESET).append(text).append(" ".repeat(maxColumnLengths[index] - toPlaintext(text).length))
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

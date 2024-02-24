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

import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URI
import java.net.URL
import java.net.URLEncoder
import java.util.function.Consumer

object HTTPRequestUtils {
    @JvmStatic
    @JvmSynthetic
    fun request(url: String, method: String = "GET", contentType: String? = null, body: Any? = null, block: (HttpURLConnection.() -> Unit)? = null): HTTPRequestResult =
        request(stringToUrl(url), method, contentType, body, block)

    @JvmStatic
    @JvmSynthetic
    fun request(url: URL, method: String = "GET", contentType: String? = null, body: Any? = null, block: (HttpURLConnection.() -> Unit)? = null): HTTPRequestResult {
        require(url.protocol == "http" || url.protocol == "https") { "Protocol of the URL should be HTTP/HTTPS, but was ${url.protocol.uppercase()}" }

        val bytes = when (body) {
            null -> null
            is ByteArray -> body
            is String -> body.toByteArray()
            is CharSequence -> body.toString().toByteArray()
            else -> throw IllegalArgumentException("Cannot convert thr argument 'body' to a byte array, type of 'body': ${body.javaClass.typeName}")
        }

        val connection: HttpURLConnection
        try {
            connection = url.openConnection() as HttpURLConnection
            try {
                connection.requestMethod = method
                if (contentType != null) {
                    connection.setRequestProperty("Content-Type", contentType)
                }
                if (bytes != null) {
                    connection.doOutput = true
                    connection.outputStream.use {
                        it.write(bytes)
                        it.flush()
                    }
                }
                block?.invoke(connection)

                val status = connection.responseCode
                return if (status >= 400) {
                    HTTPRequestResult.ErrorResponse(status, connection.errorStream?.use { it.readBytes() } ?: byteArrayOf())
                } else {
                    HTTPRequestResult.SuccessResponse(status, connection.inputStream.use { it.readBytes() })
                }
            } finally {
                connection.disconnect()
            }
        } catch (exception: Throwable) {
            return HTTPRequestResult.InterruptedByException(exception)
        }
    }

    @JvmStatic
    @JvmOverloads
    @JvmName("request")
    fun _request(url: String, method: String = "GET", contentType: String? = null, body: Any? = null, block: Consumer<in HttpURLConnection>? = null): HTTPRequestResult =
        request(url, method, contentType, body) { block?.accept(this) }

    @JvmStatic
    @JvmOverloads
    @JvmName("request")
    fun _request(url: URL, method: String = "GET", contentType: String? = null, body: Any? = null, block: Consumer<in HttpURLConnection>? = null): HTTPRequestResult =
        request(url, method, contentType, body) { block?.accept(this) }

    @JvmStatic
    fun joinParameters(baseUrl: String, params: Map<String, String>): String = baseUrl + '?' + encodeMap(params)

    @JvmStatic
    fun encodeMap(params: Map<String, String>): String {
        val builder = StringBuilder()
        for ((key, value) in params) {
            if (builder.isNotEmpty()) {
                builder.append("&")
            }
            builder.append(URLEncoder.encode(key, Charsets.UTF_8))
            builder.append("=")
            builder.append(URLEncoder.encode(value, Charsets.UTF_8))
        }
        return builder.toString()
    }

    @JvmStatic
    private fun stringToUrl(string: String): URL {
        try {
            return URI.create(string).toURL()
        } catch (exception: MalformedURLException) {
            throw IllegalArgumentException("Invalid URL: $string", exception)
        }
    }
}

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

package net.sharedwonder.lightproxy.http

import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers

object HttpUtils {
    private val httpClient = HttpClient.newHttpClient()

    @JvmStatic
    fun sendRequest(request: HttpRequest): HttpRequestResult {
        try {
            val response = httpClient.send(request, BodyHandlers.ofByteArray())
            return if (response.statusCode() >= 400) {
                HttpRequestResult.ErrorResponse(response.statusCode(), response.body())
            } else {
                HttpRequestResult.SuccessResponse(response.statusCode(), response.body())
            } 
        } catch (exception: Throwable) {
            return HttpRequestResult.FailedByException(exception)
        }
    }

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
}

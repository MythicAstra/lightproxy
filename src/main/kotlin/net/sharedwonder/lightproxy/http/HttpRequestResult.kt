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

import java.io.IOException

sealed class HttpRequestResult {
    val asResponse: Response get() = this as Response

    inline fun onResponse(block: Response.() -> Unit): HttpRequestResult {
        if (this is Response) {
            block(this)
        }
        return this
    }

    inline fun onError(block: Error.() -> Unit): HttpRequestResult {
        if (this is Error) {
            block(this)
        }
        return this
    }

    inline fun onSuccess(block: Success.() -> Unit): HttpRequestResult {
        if (this is Success) {
            block(this)
        }
        return this
    }

    inline fun onHttpError(block: HttpError.() -> Unit): HttpRequestResult {
        if (this is HttpError) {
            block(this)
        }
        return this
    }

    inline fun onIoError(block: IoError.() -> Unit): HttpRequestResult {
        if (this is IoError) {
            block(this)
        }
        return this
    }

    inline fun onInterruption(block: Interruption.() -> Unit): HttpRequestResult {
        if (this is Interruption) {
            block(this)
        }
        return this
    }

    sealed interface Response {
        val status: Int

        val content: ByteArray?

        val contentAsUtf8String: String
            get() = checkNotNull(content) { "No content available" }.toString(Charsets.UTF_8)
    }

    sealed interface Error {
        fun newException(message: String? = null): HttpRequestException
    }

    class Success(override val status: Int, override val content: ByteArray?) : HttpRequestResult(), Response

    class HttpError(override val status: Int, override val content: ByteArray?) : HttpRequestResult(), Response, Error {
        override fun newException(message: String?): HttpRequestException = HttpRequestException(message, status, contentAsUtf8String)
    }

    class IoError(val ioException: IOException) : HttpRequestResult(), Error {
        override fun newException(message: String?): HttpRequestException = HttpRequestException(message, ioException)
    }

    class Interruption(val interruptedException: InterruptedException) : HttpRequestResult(), Error {
        override fun newException(message: String?): HttpRequestException = HttpRequestException(message, interruptedException)
    }
}

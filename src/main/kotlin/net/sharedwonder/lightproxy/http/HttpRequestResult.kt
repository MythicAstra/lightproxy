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

sealed class HttpRequestResult {
    val asResponse: Response get() = this as Response

    inline fun onSuccess(block: SuccessResponse.() -> Unit): HttpRequestResult {
        if (this is SuccessResponse) {
            block(this)
        }
        return this
    }

    inline fun onFailure(block: Failure.() -> Unit): HttpRequestResult {
        if (this is Failure) {
            block(this)
        }
        return this
    }

    inline fun isResponse(block: Response.() -> Unit): HttpRequestResult {
        if (this is Response) {
            block(this)
        }
        return this
    }

    inline fun isErrorResponse(block: ErrorResponse.() -> Unit): HttpRequestResult {
        if (this is ErrorResponse) {
            block(this)
        }
        return this
    }

    inline fun whenFailedByException(block: FailedByException.() -> Unit): HttpRequestResult {
        if (this is FailedByException) {
            block(this)
        }
        return this
    }

    inline fun whenInterrupted(block: FailedByException.() -> Unit): HttpRequestResult {
        if (this is FailedByException && interrupted) {
            block(this)
        }
        return this
    }

    sealed interface Response {
        val status: Int

        val content: ByteArray?

        val contentAsUtf8String: String?
            get() = content?.toString(Charsets.UTF_8)

        val error: Boolean
    }

    sealed interface Failure {
        val interrupted: Boolean

        fun newException(message: String? = null): HttpRequestException
    }

    class SuccessResponse(override val status: Int, override val content: ByteArray?) : HttpRequestResult(), Response {
        override val error: Boolean get() = false
    }

    class ErrorResponse(override val status: Int, override val content: ByteArray?) : HttpRequestResult(), Response, Failure {
        override val error: Boolean get() = false

        override val interrupted: Boolean get() = false

        override fun newException(message: String?): HttpRequestException = HttpRequestException(message, status, contentAsUtf8String)
    }

    class FailedByException(val exception: Throwable) : HttpRequestResult(), Failure {
        override val interrupted: Boolean = exception is InterruptedException

        override fun newException(message: String?): HttpRequestException = HttpRequestException(message, exception)
    }
}

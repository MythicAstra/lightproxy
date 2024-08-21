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

package net.sharedwonder.lightproxy.http

sealed class HTTPRequestResult {
    val response: Response
        get() = if (this is Response) this else error("Not a response")

    @JvmSynthetic
    inline fun onSuccess(block: SuccessResponse.() -> Unit): HTTPRequestResult {
        if (this is SuccessResponse) {
            block(this)
        }
        return this
    }

    @JvmSynthetic
    inline fun onFailure(block: Failure.() -> Unit): HTTPRequestResult {
        if (this is Failure) {
            block(this)
        }
        return this
    }

    @JvmSynthetic
    inline fun ifErrorResponse(block: ErrorResponse.() -> Unit): HTTPRequestResult {
        if (this is ErrorResponse) {
            block(this)
        }
        return this
    }

    @JvmSynthetic
    inline fun ifInterruptedByException(block: InterruptedByException.() -> Unit): HTTPRequestResult {
        if (this is InterruptedByException) {
            block(this)
        }
        return this
    }

    sealed interface Response {
        val status: Int

        val content: ByteArray

        val contentAsUtf8String: String
            get() = content.toString(Charsets.UTF_8)
    }

    sealed interface Failure {
        fun buildException(message: String? = null): HTTPRequestException
    }

    class SuccessResponse(override val status: Int, override val content: ByteArray) : HTTPRequestResult(), Response

    class ErrorResponse(override val status: Int, override val content: ByteArray) : HTTPRequestResult(), Response, Failure {
        override fun buildException(message: String?): HTTPRequestException = HTTPRequestException(message, status, contentAsUtf8String)
    }

    class InterruptedByException(val exception: Throwable) : HTTPRequestResult(), Failure {
        override fun buildException(message: String?): HTTPRequestException = HTTPRequestException(message, exception)
    }
}

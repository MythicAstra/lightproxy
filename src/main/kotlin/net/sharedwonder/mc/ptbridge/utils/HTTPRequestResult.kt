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

sealed class HTTPRequestResult {
    val response: Response
        get() = if (this is Response) this else error("Not a response")

    @JvmSynthetic
    inline fun <R> onSuccess(block: SuccessResponse.() -> R): HTTPRequestResult {
        if (this is SuccessResponse) {
            block(this)
        }
        return this
    }

    @JvmSynthetic
    inline fun <R> onFailure(block: Failure.() -> R): HTTPRequestResult {
        if (this is Failure) {
            block(this)
        }
        return this
    }

    @JvmSynthetic
    inline fun <R> onErrorResponse(block: ErrorResponse.() -> R): HTTPRequestResult {
        if (this is ErrorResponse) {
            block(this)
        }
        return this
    }

    @JvmSynthetic
    inline fun <R> onExceptionThrown(block: ExceptionThrown.() -> R): HTTPRequestResult {
        if (this is ExceptionThrown) {
            block(this)
        }
        return this
    }

    sealed interface Response {
        val status: Int

        val content: ByteArray

        val contentAsString: String
            get() = content.toString(Charsets.UTF_8)
    }

    sealed interface Failure {
        fun buildException(errorMessage: String? = null): HTTPRequestException
    }

    class SuccessResponse(override val status: Int, override val content: ByteArray) : HTTPRequestResult(), Response

    class ErrorResponse(override val status: Int, override val content: ByteArray) : HTTPRequestResult(), Failure, Response {
        override fun buildException(errorMessage: String?): HTTPRequestException = HTTPRequestException(errorMessage, status, contentAsString)
    }

    class ExceptionThrown(val exception: Throwable) : HTTPRequestResult(), Failure {
        override fun buildException(errorMessage: String?): HTTPRequestException = HTTPRequestException(errorMessage, exception)
    }
}

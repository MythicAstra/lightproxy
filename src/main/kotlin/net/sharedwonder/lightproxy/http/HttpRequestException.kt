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

import java.io.Serial

class HttpRequestException : RuntimeException {
    val responseCode: Int?

    val responseContent: String?

    override val message: String?
        get() {
            if (responseCode == null) {
                return super.message
            }
            val info = "\n- Response code: $responseCode" + (if (responseContent != null) "\n- Response content: $responseContent" else "")
            return if (super.message == null) info else super.message + info
        }

    constructor(message: String?, responseCode: Int, responseContent: String?) : super(message) {
        this.responseCode = responseCode
        this.responseContent = responseContent
    }

    constructor(message: String?, cause: Exception?) : super(message, cause) {
        responseCode = null
        responseContent = null
    }

    private companion object {
        @Serial private const val serialVersionUID = 3653710697125111531L
    }
}

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

@file:JvmName("Constants")

package net.sharedwonder.mc.ptbridge.utils

const val PID_CH_HANDSHAKE: Int = 0x0

const val PID_CL_REQUEST_LOGIN: Int = 0x0

const val PID_CL_ENCRYPTION_RESPONSE: Int = 0x1

const val PID_SL_REQUEST_ENCRYPTION: Int = 0x1

const val PID_SL_LOGIN_SUCCESS: Int = 0x2

const val PID_SL_ENABLE_COMPRESSION: Int = 0x3

const val PID_SP_V47_SET_COMPRESSION_LEVEL: Int = 0x46

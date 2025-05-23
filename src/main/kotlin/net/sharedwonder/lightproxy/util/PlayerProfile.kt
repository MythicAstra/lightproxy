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

package net.sharedwonder.lightproxy.util

import java.lang.reflect.Type
import java.math.BigInteger
import java.net.HttpURLConnection
import java.net.URI
import java.net.http.HttpRequest
import java.util.UUID
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.google.gson.annotations.JsonAdapter
import net.sharedwonder.lightproxy.http.HttpUtils
import net.sharedwonder.lightproxy.mcauth.McAuth

@JsonAdapter(PlayerProfile.JsonAdapter::class)
data class PlayerProfile @JvmOverloads constructor(val username: String, val uuid: UUID, val auth: McAuth? = null) {
    fun joinServer(serverId: ByteArray) {
        checkNotNull(auth) { "This player profile has no authentication information" }

        val body = JsonBuilder().objectValue {
            "accessToken" value auth.accessToken
            "selectedProfile" value UuidUtils.uuidToString(uuid)
            "serverId" value BigInteger(serverId).toString(16)
        }.writer.toString()

        val request = HttpRequest.newBuilder(joinServerUri)
            .POST(HttpRequest.BodyPublishers.ofString(body)).header("Content-Type", "application/json; charset=utf-8").build()
        HttpUtils.sendRequest(request)
            .onError { throw newException("Failed to request to join the server for the player '$username/${UuidUtils.uuidToString(uuid)}' on Minecraft Session Server") }
    }

    @JvmOverloads
    fun hasJoinedServer(serverId: ByteArray, clientIp: String? = null): Boolean {
        val args = buildMap {
            put("username", username)
            put("serverId", BigInteger(serverId).toString(16))
            if (clientIp != null) {
                put("ip", clientIp)
            }
        }

        val uri = URI.create("https://sessionserver.mojang.com/session/minecraft/hasJoined?" + HttpUtils.encodeMap(args))
        val request = HttpRequest.newBuilder(uri).GET().build()
        return HttpUtils.sendRequest(request)
            .onError { throw newException("Failed to request to verify that the player '$username/${UuidUtils.uuidToString(uuid)}' has joined the server") }
            .asResponse.status.let { it == HttpURLConnection.HTTP_OK }
    }

    internal class JsonAdapter : JsonSerializer<PlayerProfile>, JsonDeserializer<PlayerProfile> {
        override fun serialize(src: PlayerProfile?, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
            if (src == null) {
                return JsonNull.INSTANCE
            }

            return JsonObject().apply {
                addProperty("username", src.username)
                addProperty("uuid", UuidUtils.uuidToString(src.uuid))
                if (src.auth != null) {
                    add("auth", JsonObject().apply {
                        addProperty("impl", src.auth.javaClass.name)
                        add("data", context.serialize(src.auth))
                    })
                }
            }
        }

        override fun deserialize(json: JsonElement, typeOfObj: Type, context: JsonDeserializationContext): PlayerProfile? {
            if (json.isJsonNull) {
                return null
            }
            if (json !is JsonObject) {
                throw JsonParseException("Invalid player profile: $json")
            }

            try {
                val username = json["username"].asString
                val uuid = UuidUtils.stringToUuid(json["uuid"].asString)

                val auth = json["auth"]?.let {
                    if (it.isJsonNull) {
                        null
                    } else if (it is JsonObject) {
                        val impl = it["impl"].asString
                        val data = it["data"]
                        try {
                            context.deserialize<McAuth>(data, Class.forName(impl))
                        } catch (exception: ClassCastException) {
                            throw JsonParseException("Not an McAuth implementation: $impl")
                        } catch (exception: ClassNotFoundException) {
                            throw JsonParseException("McAuth implementation not found: $impl")
                        }
                    } else {
                        throw JsonParseException("Not a JSON object: $it")
                    }
                }

                return PlayerProfile(username, uuid, auth)
            } catch (exception: Exception) {
                throw JsonParseException("Invalid player profile: $json", exception)
            }
        }
    }
}

private val joinServerUri = URI.create("https://sessionserver.mojang.com/session/minecraft/join")

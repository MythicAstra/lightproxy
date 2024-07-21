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

import java.lang.reflect.Type
import java.math.BigInteger
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.util.UUID
import com.google.gson.Gson
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.google.gson.annotations.JsonAdapter
import net.sharedwonder.mc.ptbridge.http.HTTPRequestUtils
import net.sharedwonder.mc.ptbridge.mcauth.MCAuth

@JsonAdapter(PlayerProfile.JsonTypeAdapter::class)
data class PlayerProfile @JvmOverloads constructor(val username: String, val uuid: UUID, val auth: MCAuth? = null) {
    fun joinServer(serverId: ByteArray) {
        if (auth == null) throw UnsupportedOperationException("This player profile has no authentication information")
        val body = JsonObject()
        body.addProperty("accessToken", auth.accessToken)
        body.addProperty("selectedProfile", uuid.toString())
        body.addProperty("serverId", BigInteger(serverId).toString(16))
        HTTPRequestUtils.request(JOIN_SERVER_URL, "POST", "application/json; charset=utf-8", GSON.toJson(body))
            .onFailure { throw buildException("Failed to request to join the server for player the '$username/${UUIDUtils.uuidToString(uuid)}' on the Minecraft session service") }
    }

    @JvmOverloads
    fun hasJoinedServer(serverId: ByteArray, clientIp: String? = null): Boolean {
        val p1 = "username" to username
        val p2 = "serverId" to BigInteger(serverId).toString(16)
        val args = if (clientIp != null) mapOf(p1, p2, "ip" to clientIp) else mapOf(p1, p2)
        return (HTTPRequestUtils.request(HTTPRequestUtils.joinParameters("https://sessionserver.mojang.com/session/minecraft/hasJoined", args))
            .onFailure { throw buildException("Failed to determine the player '$username/${UUIDUtils.uuidToString(uuid)}' has joined the server on the Minecraft session service") }
            .response).status == HttpURLConnection.HTTP_OK
    }

    internal class JsonTypeAdapter : JsonSerializer<PlayerProfile>, JsonDeserializer<PlayerProfile> {
        override fun serialize(src: PlayerProfile?, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
            if (src == null) {
                return JsonNull.INSTANCE
            }
            val json = JsonObject()
            json.addProperty("username", src.username)
            json.addProperty("uuid", src.uuid.toString())
            if (src.auth != null) {
                json.add("auth", JsonObject().apply {
                    addProperty("impl", src.auth.javaClass.typeName)
                    add("data", context.serialize(src.auth))
                })
            }
            return json
        }

        override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): PlayerProfile {
            try {
                json as JsonObject

                val username = json["username"].asString
                val uuid = UUID.fromString(json["uuid"].asString)
                val auth = json["auth"]?.let {
                    if (!it.isJsonNull) {
                        it as JsonObject
                        val impl = it["impl"].asString
                        val content = it["data"]
                        try {
                            context.deserialize<MCAuth>(content, Class.forName(impl))
                        } catch (exception: ClassCastException) {
                            throw RuntimeException("Not a MCAuth implementation: $impl")
                        } catch (exception: ClassNotFoundException) {
                            throw RuntimeException("MCAuth implementation not found: $impl")
                        }
                    } else null
                }

                return PlayerProfile(username, uuid, auth)
            } catch (exception: Exception) {
                throw JsonParseException("Invalid player profile: $json", exception)
            }
        }
    }
}

private val JOIN_SERVER_URL: URL = URI("https://sessionserver.mojang.com/session/minecraft/join").toURL()

private val GSON = Gson()

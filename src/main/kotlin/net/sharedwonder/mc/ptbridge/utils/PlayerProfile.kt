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

import net.sharedwonder.mc.ptbridge.mcauth.MCAuth
import java.lang.reflect.Type
import java.math.BigInteger
import java.net.HttpURLConnection
import java.util.UUID
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer

data class PlayerProfile @JvmOverloads constructor(val username: String, val uuid: UUID, val auth: MCAuth? = null) {
    fun joinServer(serverId: ByteArray) {
        if (auth == null) throw UnsupportedOperationException("This player profile has no authentication information")
        val body = JsonObject()
        body.addProperty("accessToken", auth.accessToken)
        body.addProperty("selectedProfile", uuid.toString())
        body.addProperty("serverId", BigInteger(serverId).toString(16))
        HTTPRequestUtils.request("https://sessionserver.mojang.com/session/minecraft/join", "POST", "application/json; charset=utf-8", GSON.toJson(body))
            .onFailure { throw buildException("Failed to request to join the server for player the '$username/${UUIDUtils.uuidToString(uuid)}' on the Minecraft session service") }
    }

    @JvmOverloads
    fun hasJoinedServer(serverId: ByteArray, playerIp: String? = null): Boolean {
        val p1 = "username" to username
        val p2 = "serverId" to BigInteger(serverId).toString(16)
        val args = if (playerIp != null) mapOf(p1, p2, "ip" to playerIp) else mapOf(p1, p2)
        return (HTTPRequestUtils.request(HTTPRequestUtils.joinParameters("https://sessionserver.mojang.com/session/minecraft/hasJoined", args)).onFailure {
            throw buildException("Failed to request whether the player '$username/${UUIDUtils.uuidToString(uuid)}' has joined the server on the Minecraft session service")
        }.response).status == HttpURLConnection.HTTP_OK
    }

    class Serializer : JsonSerializer<PlayerProfile> {
        override fun serialize(src: PlayerProfile, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
            val json = JsonObject()
            json.addProperty("username", src.username)
            json.addProperty("uuid", src.uuid.toString())
            if (src.auth != null) {
                json.add("auth", JsonObject().apply {
                    addProperty("impl", src.auth.javaClass.name)
                    add("data", context.serialize(src.auth))
                })
            }
            return json
        }
    }

    class Deserializer : JsonDeserializer<PlayerProfile> {
        override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): PlayerProfile {
            try {
                json as JsonObject

                val username = json.get("username")!!.asString
                val uuid = UUID.fromString(json.get("uuid")!!.asString)
                val auth = json.get("auth")?.let {
                    if (it.isJsonNull) {
                        null
                    } else {
                        it as JsonObject
                        val impl = it.get("impl")!!.asString
                        val content = it.get("data")!!
                        try {
                            context.deserialize<MCAuth>(content, Class.forName(impl))
                        } catch (exception: ClassCastException) {
                            throw RuntimeException("Not a MCAuth implementation: $impl")
                        } catch (exception: ClassNotFoundException) {
                            throw RuntimeException("MCAuth implementation not found: $impl")
                        }
                    }
                }

                return PlayerProfile(username, uuid, auth)
            } catch (exception: Exception) {
                throw JsonParseException("Invalid player profile: $json", exception)
            }
        }
    }
}

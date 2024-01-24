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

package net.sharedwonder.mc.ptbridge.mcauth.msa

import net.sharedwonder.mc.ptbridge.mcauth.MCAuth
import net.sharedwonder.mc.ptbridge.utils.GSON
import net.sharedwonder.mc.ptbridge.utils.HTTPRequestUtils
import net.sharedwonder.mc.ptbridge.utils.PlayerProfile
import net.sharedwonder.mc.ptbridge.utils.UUIDUtils
import java.net.URI
import com.google.gson.JsonArray
import com.google.gson.JsonObject

class MCAuthWithMSA(tokenType: MSAAuthTokenType, authToken: String) : MCAuth {
    override val accessToken: String

    override val expirationTime: Long

    val msaRefreshToken: String

    init {
        val (msaAccessToken, msaRefreshToken) = msaAuthStep(tokenType, authToken)
        val (xboxLiveToken, xboxUserHash) = xboxLiveAuthStep(msaAccessToken)
        val xstsToken = xstsAuthStep(xboxLiveToken)

        val startTime = System.currentTimeMillis()
        val (accessToken, expiresIn) = mcAuthStep(xstsToken, xboxUserHash)

        this.accessToken = accessToken
        expirationTime = startTime + expiresIn * 1000
        this.msaRefreshToken = msaRefreshToken
    }

    override fun refresh(): MCAuthWithMSA = MCAuthWithMSA(MSAAuthTokenType.REFRESH_TOKEN, msaRefreshToken)

    override fun createProfile(): PlayerProfile {
        val response = HTTPRequestUtils.request(MC_QUERY_PROFILE_URL) {
            setRequestProperty("Authorization", "Bearer $accessToken")
        }.onFailure {
            throw buildException("Failed to query the player profile")
        }.response.contentAsString

        val jsonResponse = GSON.fromJson(response, JsonObject::class.java)
        return PlayerProfile(jsonResponse["name"].asString, UUIDUtils.stringToUuid(jsonResponse["id"].asString), this)
    }
}

private val MSA_AUTH_URL = URI("https://login.live.com/oauth20_token.srf").toURL()

private val XBOX_LIVE_AUTH_URL = URI("https://user.auth.xboxlive.com/user/authenticate").toURL()

private val XSTS_AUTH_URL = URI("https://xsts.auth.xboxlive.com/xsts/authorize").toURL()

private val MC_AUTH_URL = URI("https://api.minecraftservices.com/authentication/login_with_xbox").toURL()

private val MC_QUERY_PROFILE_URL = URI("https://api.minecraftservices.com/minecraft/profile").toURL()

private fun msaAuthStep(tokenType: MSAAuthTokenType, authToken: String): MSAAuthResponse {
    val body = mapOf(
        "client_id" to "00000000402b5328",
        "grant_type" to tokenType.value,
        "scope" to "service::user.auth.xboxlive.com::MBI_SSL",
        "redirect_uri" to "https://login.live.com/oauth20_desktop.srf",
        tokenType.queryParamName to authToken
    )

    val content = HTTPRequestUtils.request(MSA_AUTH_URL, "POST", "application/x-www-form-urlencoded; charset=utf-8", HTTPRequestUtils.encodeParameters(body))
        .onFailure { throw buildException("Failed to get the MSA access token") }.response.contentAsString
    return GSON.fromJson(content, MSAAuthResponse::class.java)
}

private fun xboxLiveAuthStep(msaToken: String): Pair<String, String> {
    val body = JsonObject()
    body.add("Properties", JsonObject().apply {
        addProperty("AuthMethod", "RPS")
        addProperty("SiteName", "user.auth.xboxlive.com")
        addProperty("RpsTicket", msaToken)
    })
    body.addProperty("RelyingParty", "http://auth.xboxlive.com")
    body.addProperty("TokenType", "JWT")

    val content = HTTPRequestUtils.request(XBOX_LIVE_AUTH_URL, "POST", "application/json; charset=utf-8", GSON.toJson(body))
        .onFailure { throw buildException("Failed to get the Xbox access token") }.response.contentAsString
    val json = GSON.fromJson(content, JsonObject::class.java)

    val token = json["Token"].asString
    val userHash = json["DisplayClaims"].asJsonObject["xui"].asJsonArray[0].asJsonObject["uhs"].asString
    return token to userHash
}

private fun xstsAuthStep(xboxToken: String): String {
    val body = JsonObject()
    body.add("Properties", JsonObject().apply {
        addProperty("SandboxId", "RETAIL")
        add("UserTokens", JsonArray().apply {
            add(xboxToken)
        })
    })
    body.addProperty("RelyingParty", "rp://api.minecraftservices.com/")
    body.addProperty("TokenType", "JWT")

    val content = HTTPRequestUtils.request(XSTS_AUTH_URL, "POST", "application/json; charset=utf-8", GSON.toJson(body))
        .onFailure { throw buildException("Failed to get the XSTS access token") }.response.contentAsString
    return GSON.fromJson(content, JsonObject::class.java)["Token"].asString
}

private fun mcAuthStep(xstsToken: String, xboxUserHash: String): MCAuthResponse {
    val body = JsonObject()
    body.addProperty("identityToken", "XBL3.0 x=$xboxUserHash;$xstsToken")

    val content = HTTPRequestUtils.request(MC_AUTH_URL, "POST", "application/json; charset=utf-8", GSON.toJson(body))
        .onFailure { throw buildException("Failed to get the Minecraft access token") }.response.contentAsString

    return GSON.fromJson(content, MCAuthResponse::class.java)
}

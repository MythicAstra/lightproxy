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

package net.sharedwonder.lightproxy.mcauth.msa

import java.net.URI
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import net.sharedwonder.lightproxy.http.HTTPRequestUtils
import net.sharedwonder.lightproxy.mcauth.MCAuth
import net.sharedwonder.lightproxy.util.PlayerProfile
import net.sharedwonder.lightproxy.util.UUIDUtils

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
        val response = HTTPRequestUtils.request(mcQueryProfileUrl) {
            setRequestProperty("Authorization", "Bearer $accessToken")
        }.onFailure {
            throw buildException("Failed to query the player profile")
        }.response.contentAsUtf8String

        val jsonResponse = gson.fromJson(response, JsonObject::class.java)
        return PlayerProfile(jsonResponse["name"].asString, UUIDUtils.stringToUuid(jsonResponse["id"].asString), this)
    }
}

private val msaAuthUrl = URI("https://login.live.com/oauth20_token.srf").toURL()

private val xboxLiveAuthUrl = URI("https://user.auth.xboxlive.com/user/authenticate").toURL()

private val xstsAuthUrl = URI("https://xsts.auth.xboxlive.com/xsts/authorize").toURL()

private val mcAuthUrl = URI("https://api.minecraftservices.com/authentication/login_with_xbox").toURL()

private val mcQueryProfileUrl = URI("https://api.minecraftservices.com/minecraft/profile").toURL()

private val gson = Gson()

private fun msaAuthStep(tokenType: MSAAuthTokenType, authToken: String): MSAAuthResponse {
    val body = mapOf(
        "client_id" to "00000000402b5328",
        "grant_type" to tokenType.value,
        "scope" to "service::user.auth.xboxlive.com::MBI_SSL",
        "redirect_uri" to "https://login.live.com/oauth20_desktop.srf",
        tokenType.queryParamName to authToken
    )

    val content = HTTPRequestUtils.request(msaAuthUrl, "POST", "application/x-www-form-urlencoded; charset=utf-8", HTTPRequestUtils.encodeMap(body))
        .onFailure { throw buildException("Failed to get the MSA access token") }.response.contentAsUtf8String
    return gson.fromJson(content, MSAAuthResponse::class.java)
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

    val content = HTTPRequestUtils.request(xboxLiveAuthUrl, "POST", "application/json; charset=utf-8", gson.toJson(body))
        .onFailure { throw buildException("Failed to get the Xbox access token") }.response.contentAsUtf8String
    val map = gson.fromJson(content, Map::class.java)

    val token = map["Token"] as String
    val userHash = (((map["DisplayClaims"] as Map<*, *>)["xui"] as List<*>)[0] as Map<*, *>)["uhs"] as String
    return Pair(token, userHash)
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

    val content = HTTPRequestUtils.request(xstsAuthUrl, "POST", "application/json; charset=utf-8", gson.toJson(body))
        .onFailure { throw buildException("Failed to get the XSTS access token") }.response.contentAsUtf8String
    return gson.fromJson(content, Map::class.java)["Token"] as String
}

private fun mcAuthStep(xstsToken: String, xboxUserHash: String): MCAuthResponse {
    val body = JsonObject()
    body.addProperty("identityToken", "XBL3.0 x=$xboxUserHash;$xstsToken")

    val content = HTTPRequestUtils.request(mcAuthUrl, "POST", "application/json; charset=utf-8", gson.toJson(body))
        .onFailure { throw buildException("Failed to get the Minecraft access token") }.response.contentAsUtf8String

    return gson.fromJson(content, MCAuthResponse::class.java)
}

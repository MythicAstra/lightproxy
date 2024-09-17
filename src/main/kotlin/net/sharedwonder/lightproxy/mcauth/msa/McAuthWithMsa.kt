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
import java.net.http.HttpRequest
import net.sharedwonder.lightproxy.http.HttpUtils
import net.sharedwonder.lightproxy.mcauth.McAuth
import net.sharedwonder.lightproxy.util.JsonBuilder
import net.sharedwonder.lightproxy.util.JsonUtils
import net.sharedwonder.lightproxy.util.PlayerProfile
import net.sharedwonder.lightproxy.util.UuidUtils

class McAuthWithMsa(tokenType: MsaAuthTokenType, authToken: String) : McAuth {
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

    override fun refresh(): McAuthWithMsa = McAuthWithMsa(MsaAuthTokenType.REFRESH_TOKEN, msaRefreshToken)

    override fun createProfile(): PlayerProfile {
        val response = HttpUtils.request(HttpRequest.newBuilder(mcAccountProfileUri).GET().header("Authorization", "Bearer $accessToken").build())
            .onFailure { throw buildException("Failed to fetch the Minecraft account profile") }.asResponse.contentAsUtf8String

        val map = JsonUtils.fromJson<Map<*, *>>(response)
        return PlayerProfile(map["name"] as String, UuidUtils.stringToUuid(map["id"] as String), this)
    }
}

private val msaAuthUri = URI("https://login.live.com/oauth20_token.srf")

private val xboxLiveAuthUri = URI("https://user.auth.xboxlive.com/user/authenticate")

private val xstsAuthUri = URI("https://xsts.auth.xboxlive.com/xsts/authorize")

private val mcAuthUri = URI("https://api.minecraftservices.com/authentication/login_with_xbox")

private val mcAccountProfileUri = URI("https://api.minecraftservices.com/minecraft/profile")

private fun msaAuthStep(tokenType: MsaAuthTokenType, authToken: String): MsaAuthResponse {
    val body = mapOf(
        "client_id" to "00000000402b5328",
        "grant_type" to tokenType.value,
        "scope" to "service::user.auth.xboxlive.com::MBI_SSL",
        "redirect_uri" to "https://login.live.com/oauth20_desktop.srf",
        tokenType.queryParamName to authToken
    )

    val content = HttpUtils.request(
        HttpRequest.newBuilder(msaAuthUri)
            .POST(HttpRequest.BodyPublishers.ofString(HttpUtils.encodeMap(body)))
            .header("Content-Type", "application/x-www-form-urlencoded; charset=utf-8")
            .build()
    ).onFailure { throw buildException("Failed to get the MSA access token") }.asResponse.contentAsUtf8String

    return JsonUtils.fromJson<MsaAuthResponse>(content)
}

private fun xboxLiveAuthStep(msaToken: String): Pair<String, String> {
    val body = JsonBuilder().objectValue {
        "Properties" objectValue {
            "AuthMethod" value "RPS"
            "SiteName" value "user.auth.xboxlive.com"
            "RpsTicket" value msaToken
        }
        "RelyingParty" value "http://auth.xboxlive.com"
        "TokenType" value "JWT"
    }.toString()

    val content = HttpUtils.request(
        HttpRequest.newBuilder(xboxLiveAuthUri)
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .header("Content-Type", "application/json; charset=utf-8")
            .build()
    ).onFailure { throw buildException("Failed to get the Xbox access token") }.asResponse.contentAsUtf8String

    val map = JsonUtils.fromJson<Map<*, *>>(content)
    val token = map["Token"] as String
    val userHash = (((map["DisplayClaims"] as Map<*, *>)["xui"] as List<*>)[0] as Map<*, *>)["uhs"] as String
    return Pair(token, userHash)
}

private fun xstsAuthStep(xboxToken: String): String {
    val body = JsonBuilder().objectValue {
        "Properties".objectValue {
            "SandboxId" value "RETAIL"
            "UserTokens" arrayValue { value(xboxToken) }
        }
        "RelyingParty" value "rp://api.minecraftservices.com/"
        "TokenType" value "JWT"
    }.toString()

    val content = HttpUtils.request(
        HttpRequest.newBuilder(xstsAuthUri)
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .header("Content-Type", "application/json; charset=utf-8")
            .build()
    ).onFailure { throw buildException("Failed to get the XSTS access token") }.asResponse.contentAsUtf8String

    return JsonUtils.fromJson<Map<*, *>>(content)["Token"] as String
}

private fun mcAuthStep(xstsToken: String, xboxUserHash: String): McAuthResponse {
    val body = JsonBuilder().objectValue { entry("identityToken", "XBL3.0 x=$xboxUserHash;$xstsToken") }.toString()

    val content = HttpUtils.request(
        HttpRequest.newBuilder(mcAuthUri)
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .header("Content-Type", "application/json; charset=utf-8")
            .build()
    ).onFailure { throw buildException("Failed to get the Minecraft access token") }.asResponse.contentAsUtf8String

    return JsonUtils.fromJson<McAuthResponse>(content)
}

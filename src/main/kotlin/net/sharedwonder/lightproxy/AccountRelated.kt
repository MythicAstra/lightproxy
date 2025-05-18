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

package net.sharedwonder.lightproxy

import java.io.File
import java.util.regex.Pattern
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonWriter
import net.sharedwonder.lightproxy.mcauth.msa.McAuthWithMsa
import net.sharedwonder.lightproxy.mcauth.msa.MsaAuthTokenType
import net.sharedwonder.lightproxy.util.JsonUtils
import net.sharedwonder.lightproxy.util.PlayerProfile
import net.sharedwonder.lightproxy.util.UuidUtils

typealias AccountMap = Map<String, PlayerProfile>

typealias MutableAccountMap = MutableMap<String, PlayerProfile>

private val msaRedirectUriPattern = Pattern.compile("^\\s*http(s)?://login\\.live\\.com/oauth20_desktop\\.srf\\?code=([^&\\s]+)(.+)?\$")

private val accountMapTypeToken = TypeToken.getParameterized(Map::class.java, String::class.java, PlayerProfile::class.java)

internal fun readAccountFile(file: File): MutableAccountMap =
    if (file.isFile) {
        file.reader().use {
            @Suppress("unchecked_cast")
            JsonUtils.fromJson(it, accountMapTypeToken) as MutableAccountMap
        }
    } else {
        mutableMapOf()
    }

internal fun writeAccountFile(file: File, accounts: AccountMap) {
    JsonWriter(file.writer()).use {
        it.setIndent("    ")
        JsonUtils.toJson(accounts, Map::class.java, it)
    }
}

internal fun addAccount(accountFile: File): Int {
    try {
        println("Open 'https://login.live.com/oauth20_authorize.srf?client_id=00000000402b5328&response_type=code" +
            "&scope=service::user.auth.xboxlive.com::MBI_SSL&redirect_uri=https://login.live.com/oauth20_desktop.srf' to login your Microsoft Account.")
        println("Then input the authorization code or the redirect URI here:")

        val input = readlnOrNull()
        if (input.isNullOrBlank()) {
            System.err.println("You didn't input anything")
            return 1
        }
        val matcher = msaRedirectUriPattern.matcher(input)
        val code = if (matcher.matches()) matcher.group(2) else input.trim(Char::isWhitespace)

        val profile = McAuthWithMsa(MsaAuthTokenType.AUTHORIZATION_CODE, code).profile()
        val accounts = readAccountFile(accountFile)
        var override = false
        if (accounts.containsKey(profile.username)) {
            override = true
        }
        accounts[profile.username] = profile
        writeAccountFile(accountFile, accounts)

        if (override) {
            println("Successfully updated the Minecraft account: ${profile.username}/${UuidUtils.uuidToString(profile.uuid)}")
        } else {
            println("Successfully added a new Minecraft account: ${profile.username}/${UuidUtils.uuidToString(profile.uuid)}")
        }
        return 0
    } catch (exception: Throwable) {
        System.err.println("An error occurred while adding a Minecraft account")
        exception.printStackTrace()
        return 1
    }
}

internal fun removeAccount(accountFile: File, username: String): Int {
    try {
        val accounts = readAccountFile(accountFile)
        if (accounts.remove(username) == null) {
            System.err.println("Cannot find the Minecraft account with the username '$username'")
            return 1
        }
        writeAccountFile(accountFile, accounts)
        println("Successfully removed the specified Minecraft account")
        return 0
    } catch (exception: Throwable) {
        System.err.println("An error occurred while removing a Minecraft account")
        exception.printStackTrace()
        return 1
    }
}

internal fun listAccounts(accountFile: File): Int {
    try {
        val accounts = readAccountFile(accountFile)
        for ((_, profile) in accounts) {
            println("${profile.username}/${profile.uuid}")
        }
        return 0
    } catch (exception: Throwable) {
        System.err.println("An error occurred while listing Minecraft accounts")
        exception.printStackTrace()
        return 1
    }
}

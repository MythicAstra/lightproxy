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

package net.sharedwonder.lightproxy

import java.io.File
import java.util.regex.Pattern
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import net.sharedwonder.lightproxy.mcauth.msa.MCAuthWithMSA
import net.sharedwonder.lightproxy.mcauth.msa.MSAAuthTokenType
import net.sharedwonder.lightproxy.util.PlayerProfile
import net.sharedwonder.lightproxy.util.UUIDUtils

private const val MSA_LOGIN_URL = "https://login.live.com/oauth20_authorize.srf?client_id=00000000402b5328&response_type=code" +
    "&scope=service::user.auth.xboxlive.com::MBI_SSL&redirect_uri=https://login.live.com/oauth20_desktop.srf"

private val msaLoginRedirectUrlPattern = Pattern.compile("^http(s)?://login\\.live\\.com/oauth20_desktop\\.srf\\?code=([^&]+)(&.+)?\$")

private val gson = Gson()

internal fun readAccountsFromFile(file: File): MutableMap<String, PlayerProfile> {
    return if (file.isFile) {
        file.reader().use {
            @Suppress("UNCHECKED_CAST")
            gson.fromJson(it, TypeToken.getParameterized(Map::class.java, String::class.java, PlayerProfile::class.java)) as MutableMap<String, PlayerProfile>
        }
    } else mutableMapOf()
}

internal fun writeAccountsToFile(file: File, accounts: Map<String, PlayerProfile>) {
    val jsonWriter = gson.newJsonWriter(file.writer())
    jsonWriter.setIndent("    ")
    jsonWriter.use { gson.toJson(accounts, Map::class.java, it) }
}

internal fun addMinecraftAccount(accountFile: File): Int {
    try {
        println("Open '$MSA_LOGIN_URL' to login your Microsoft Account.")
        println("Then type the authorization code or the redirect URL here:")
        val input = readlnOrNull()
        if (input.isNullOrBlank()) {
            System.err.println("You didn't input anything")
            return 1
        }

        val matcher = msaLoginRedirectUrlPattern.matcher(input)
        val code = if (matcher.matches()) matcher.group(2) else input

        val profile = MCAuthWithMSA(MSAAuthTokenType.AUTHORIZATION_CODE, code).createProfile()
        val accounts = readAccountsFromFile(accountFile)
        if (accounts.containsKey(profile.username)) {
            System.err.println("Account of the username '${profile.username}' already exists")
            return 1
        }
        accounts[profile.username] = profile
        writeAccountsToFile(accountFile, accounts)

        println("Successfully added a new Minecraft account: ${profile.username}/${UUIDUtils.uuidToString(profile.uuid)}")
        return 0
    } catch (exception: Throwable) {
        System.err.println("An error occurred while adding a Minecraft account")
        exception.printStackTrace()
        return 1
    }
}

internal fun removeMinecraftAccount(accountFile: File, username: String): Int {
    try {
        val accounts = readAccountsFromFile(accountFile)
        if (accounts.remove(username) == null) {
            System.err.println("Account of the username '$username' was not found")
        } else {
            writeAccountsToFile(accountFile, accounts)
        }

        println("Successfully removed the specified Minecraft account")
        return 0
    } catch (exception: Throwable) {
        System.err.println("An error occurred while removing a Minecraft account")
        exception.printStackTrace()
        return 1
    }
}

internal fun listMinecraftAccounts(accountFile: File): Int {
    try {
        val accounts = readAccountsFromFile(accountFile)
        for ((_, profile) in accounts) {
            println("${profile.username}/${profile.uuid}")
        }
        return 0
    } catch (exception: Throwable) {
        exception.printStackTrace()
        return 1
    }
}

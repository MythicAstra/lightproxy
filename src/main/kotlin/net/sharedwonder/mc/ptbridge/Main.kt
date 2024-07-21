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

@file:JvmName("Main")

package net.sharedwonder.mc.ptbridge

import kotlin.system.exitProcess
import java.io.File
import com.beust.jcommander.JCommander
import com.beust.jcommander.Parameter
import com.beust.jcommander.ParameterException
import com.beust.jcommander.UnixStyleUsageFormatter

private object MainArguments {
    @Parameter(names = ["-h", "--help"], help = true, description = "Show the help message")
    var help: Boolean = false

    @Parameter(names = ["-v", "--version"], description = "Show the version")
    var version: Boolean = false

    @Parameter(names = ["-b", "--bind-port"], description = "Specifies the proxy server binding port")
    var bindPort: Int = 25565

    @Parameter(names = ["-a", "--address"], description = "Specifies the origin Minecraft server address")
    var remoteAddress: String? = null

    @Parameter(names = ["-p", "--port"], description = "Specifies the origin Minecraft server port")
    var remotePort: Int = 25565

    @Parameter(names = ["-u", "--accounts-file"], description = "Specifies the Minecraft accounts file")
    var accountsFile: String = "accounts.json"

    @Parameter(names = ["-d", "--addons-dir"], description = "Specifies the directory where the addons will be loaded")
    var addonsDir: String = "addons"

    @Parameter(names = ["-c", "--config-dir"], description = "Specifies the configuration directory")
    var configDir: String = "config"

    @Parameter(names = ["-i", "--login"], description = "Adds a Minecraft account to the accounts file")
    var login: Boolean = false

    @Parameter(names = ["-o", "--logout"], description = "Remove a Minecraft account from the accounts file, requires a username")
    var logout: String? = null

    @Parameter(names = ["-l", "--list-accounts"], description = "Lists the Minecraft accounts that are logged in")
    var listAccounts: Boolean = false
}

fun main(args: Array<String>) {
    val commander = JCommander()
    commander.addObject(MainArguments)
    commander.programName = MetaInfo.ID
    try {
        commander.parse(*args)
    } catch (exception: ParameterException) {
        println("Invalid arguments: ${exception.message}")
        exitProcess(-1)
    }

    exitProcess(MainArguments.run {
        when {
            help -> showHelp(commander)
            version -> showVersion()
            login -> addMinecraftAccount(File(accountsFile))
            logout != null -> removeMinecraftAccount(File(accountsFile), logout!!)
            listAccounts -> listMinecraftAccounts(File(accountsFile))
            remoteAddress != null -> ProxyServer.start(bindPort, remoteAddress!!, remotePort, File(accountsFile), File(addonsDir), File(configDir))
            else -> showHelp(commander)
        }
    })
}

private fun showHelp(commander: JCommander): Int {
    println("PTBridge - A Lightweight Minecraft Server Proxy")
    println("Copyright (C) 2024 sharedwonder (Liu Baihao).")
    println()
    val builder = StringBuilder()
    UnixStyleUsageFormatter(commander).usage(builder)
    println(builder.toString())
    return 0
}

private fun showVersion(): Int {
    println("PTBridge version ${MetaInfo.VERSION}")
    return 0
}

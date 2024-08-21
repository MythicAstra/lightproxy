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
import java.util.Hashtable
import javax.naming.directory.DirContext
import javax.naming.directory.InitialDirContext
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import net.sharedwonder.lightproxy.addon.AddonLoader
import net.sharedwonder.lightproxy.config.ConfigManager
import net.sharedwonder.lightproxy.util.PlayerProfile
import org.apache.logging.log4j.LogManager

class LightProxy(val bindPort: Int, host: String, port: Int, accountFile: File, addonDir: File, configDir: File) {
    val remoteAddress: String

    val remotePort: Int

    val accounts: MutableMap<String, PlayerProfile>?

    init {
        try {
            ConfigManager.init(configDir)
            AddonLoader.init(addonDir)

            var accounts: MutableMap<String, PlayerProfile>? = null
            if (accountFile.isFile) {
                try {
                    accounts = readAccountsFromFile(accountFile)
                } catch (exception: Exception) {
                    logger.error("An error occurred while reading the accounts file: $exception")
                }
                if (accounts != null && refreshTokensIfExpired(accounts)) {
                    try {
                        writeAccountsToFile(accountFile, accounts)
                    } catch (exception: Exception) {
                        logger.error("An error occurred while writing the accounts file: $exception")
                    }
                }
            }
            this.accounts = accounts

            val lookupResult =
                if (port == DEFAULT_PORT) {
                    try {
                        lookupServer(host)
                    } catch (exception: Exception) {
                        null
                    }
                } else null
            remoteAddress = lookupResult?.first ?: host
            remotePort = lookupResult?.second ?: port

            logger.info(fun(): String = "Remote server: ${if (':' in remoteAddress) "[$remoteAddress]" else remoteAddress}:$remotePort")
        } catch (exception: Throwable) {
            logger.fatal("A fatal exception occurred when initializing", exception)
            throw RuntimeException(exception)
        }
    }

    fun run(): Int {
        val bossGroup = NioEventLoopGroup()
        val workerGroup = NioEventLoopGroup()

        try {
            val serverBootstrap = ServerBootstrap()
            serverBootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel::class.java)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childHandler(object : ChannelInitializer<SocketChannel>() {
                    override fun initChannel(channel: SocketChannel) {
                        channel.pipeline().addLast(ProxyServerHandler(this@LightProxy))
                    }
                })

            val channelFuture = serverBootstrap.bind(bindPort).sync()
            logger.info(fun(): String = "Listening on the port $bindPort")
            channelFuture.channel().closeFuture().sync()
            return 0
        } catch (exception: Throwable) {
            logger.fatal("A fatal exception occurred", exception)
            return 1
        } finally {
            workerGroup.shutdownGracefully()
            bossGroup.shutdownGracefully()
        }
    }

    companion object {
        private val logger = LogManager.getLogger(LightProxy::class.java)

        @JvmStatic
        fun start(bindPort: Int, remoteAddress: String, remotePort: Int, accountFile: File, addonDir: File, configDir: File): Int {
            logger.info(fun(): String = "Starting LightProxy (version ${MetaInfo.VERSION})...")

            val server: LightProxy
            try {
                server = LightProxy(bindPort, remoteAddress, remotePort, accountFile, addonDir, configDir)
            } catch (exception: RuntimeException) {
                return 2
            }
            return server.run()
        }

        private fun lookupServer(hostname: String): Pair<String, Int> {
            val environment = Hashtable<String, String>()
            environment[DirContext.INITIAL_CONTEXT_FACTORY] = "com.sun.jndi.dns.DnsContextFactory"
            val dirContext = InitialDirContext(environment)
            val domain = "_minecraft._tcp.$hostname"
            val attributes = dirContext.getAttributes(domain, arrayOf("SRV"))
            dirContext.close()

            val srv = attributes["srv"].get().toString()
            logger.info(fun(): String = "Found a SRV record on $domain: $srv")

            val content = srv.split(' ')
            return Pair(content[3], content[2].toInt())
        }

        private fun refreshTokensIfExpired(accounts: MutableMap<String, PlayerProfile>): Boolean {
            var modified = false
            for (entry in accounts) {
                val auth = entry.value.auth
                if (System.currentTimeMillis() >= (auth ?: continue).expirationTime) {
                    modified = true
                    logger.info(fun(): String = "The access token of the Minecraft account '${entry.key}' is expired, refreshing...")
                    try {
                        entry.setValue(auth.refresh().createProfile())
                    } catch (exception: RuntimeException) {
                        logger.error("Failed to refresh the access token of the Minecraft account '${entry.key}'", exception)
                    }
                }
            }
            return modified
        }
    }
}

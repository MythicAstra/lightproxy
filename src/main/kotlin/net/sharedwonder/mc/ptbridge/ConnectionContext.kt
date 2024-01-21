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

package net.sharedwonder.mc.ptbridge

import net.sharedwonder.mc.ptbridge.addon.ExternalContext
import net.sharedwonder.mc.ptbridge.crypt.EncryptionContext
import net.sharedwonder.mc.ptbridge.utils.ConnectionState
import net.sharedwonder.mc.ptbridge.utils.PlayerProfile
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set
import java.util.Queue
import java.util.concurrent.LinkedBlockingQueue
import java.util.function.Function
import java.util.function.Supplier
import io.netty.buffer.ByteBuf
import org.apache.logging.log4j.LogManager

class ConnectionContext internal constructor(proxyServer: ProxyServer) {
    @JvmField val bindPort: Int = proxyServer.bindPort

    @JvmField val remoteAddress: String = proxyServer.remoteAddress

    @JvmField val remotePort: Int = proxyServer.remotePort

    @JvmField val minecraftAccounts: Map<String, PlayerProfile>? = proxyServer.minecraftAccounts

    @JvmField internal val toServerPacketQueue: Queue<ByteBuf> = LinkedBlockingQueue()

    @JvmField internal val toClientPacketQueue: Queue<ByteBuf> = LinkedBlockingQueue()

    private val externalContexts: Map<Class<out ExternalContext>, ExternalContext> = buildMap {
        for ((type, generator) in EXTERNAL_CONTEXT_TYPES) {
            put(type, generator(this@ConnectionContext))
        }
    }

    var encryptionContext: EncryptionContext = EncryptionContext.disabled()

    var connectionState: ConnectionState = ConnectionState.HANDSHAKE

    var compressionThreshold: Int = -1

    private var _clientAddress: String? = null
    var clientAddress: String
        get() = checkNotNull(_clientAddress) { "clientAddress is not set" }
        set(value) {
            _clientAddress = value
        }

    private var _protocolVersion: Int? = null
    var protocolVersion: Int
        get() = checkNotNull(_protocolVersion) { "protocolVersion is not set" }
        set(value) {
            _protocolVersion = value
        }

    private var _playerUsername: String? = null
    var playerUsername: String
        get() = checkNotNull(_playerUsername) { "playerUsername is not set" }
        set(value) {
            _playerUsername = value
        }

    fun <T : ExternalContext> getExternalContext(type: Class<T>): T = type.cast(externalContexts[type])

    fun sendToClient(packet: ByteBuf) {
        toClientPacketQueue.add(packet)
    }

    fun sendToServer(packet: ByteBuf) {
        toServerPacketQueue.add(packet)
    }

    @PublishedApi
    internal fun onConnect() {
        val threads = ArrayList<Thread>(externalContexts.size)
        for (externalContext in externalContexts.values) {
            threads.add(Thread {
                try {
                    externalContext.onConnect()
                } catch (exception: Throwable) {
                    LOGGER.error("An error occurred while calling onConnect on ${externalContext.javaClass.typeName}", exception)
                }
            }.apply { start() })
        }
        threads.forEach { it.join() }
    }

    @PublishedApi
    internal fun onDisconnect() {
        val threads = ArrayList<Thread>(externalContexts.size)
        for (externalContext in externalContexts.values) {
            threads.add(Thread {
                try {
                    externalContext.onDisconnect()
                } catch (exception: Throwable) {
                    LOGGER.error("An error occurred while calling onDisconnect on ${externalContext.javaClass.typeName}", exception)
                }
            }.apply { start() })
        }
        threads.forEach { it.join() }
    }

    companion object {
        private val LOGGER = LogManager.getLogger(ConnectionContext::class.java)

        private val EXTERNAL_CONTEXT_TYPES: MutableMap<Class<out ExternalContext>, (ConnectionContext) -> ExternalContext> = HashMap()

        @JvmStatic
        @JvmSynthetic
        fun <T : ExternalContext> registerExternalContextType(type: Class<T>, generator: (ConnectionContext) -> T) {
            EXTERNAL_CONTEXT_TYPES[type] = generator
        }

        @JvmStatic
        fun <T : ExternalContext> registerExternalContextType(type: Class<T>, generator: Supplier<out T>) {
            EXTERNAL_CONTEXT_TYPES[type] = { generator.get() }
        }

        @JvmStatic
        fun <T : ExternalContext> registerExternalContextType(type: Class<T>, generator: Function<ConnectionContext, out T>) {
            EXTERNAL_CONTEXT_TYPES[type] = generator::apply
        }

        @JvmStatic
        fun <T : ExternalContext> unregisterExternalContextType(type: Class<T>) {
            EXTERNAL_CONTEXT_TYPES.remove(type)
        }
    }
}

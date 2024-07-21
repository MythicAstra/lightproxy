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

import java.util.Queue
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.function.Function
import java.util.function.Supplier
import io.netty.buffer.ByteBuf
import net.sharedwonder.mc.ptbridge.addon.ExternalContext
import net.sharedwonder.mc.ptbridge.crypt.EncryptionContext
import net.sharedwonder.mc.ptbridge.utils.ConnectionState
import net.sharedwonder.mc.ptbridge.utils.PlayerProfile
import org.apache.logging.log4j.LogManager

class ConnectionContext internal constructor(proxyServer: ProxyServer) {
    @JvmField val bindPort: Int = proxyServer.bindPort

    @JvmField val remoteAddress: String = proxyServer.remoteAddress

    @JvmField val remotePort: Int = proxyServer.remotePort

    @JvmField val accounts: Map<String, PlayerProfile>? = proxyServer.accounts

    @JvmField val attachedC2SPackets: Queue<ByteBuf> = ConcurrentLinkedQueue()

    @JvmField val attachedS2CPackets: Queue<ByteBuf> = ConcurrentLinkedQueue()

    val externalContexts: Map<Class<out ExternalContext>, ExternalContext> = buildMap {
        for ((type, generator) in EXTERNAL_CONTEXT_TYPES) {
            put(type, generator.apply(this@ConnectionContext))
        }
    }

    var state: ConnectionState = ConnectionState.HANDSHAKE

    var encryptionContext: EncryptionContext = EncryptionContext.disabled()

    var compressionThreshold: Int = -1

    private var _clientAddress: String? = null
    var clientAddress: String
        get() = checkNotNull(_clientAddress) { "clientAddress is not set" }
        set(value) {
            check(_clientAddress == null)
            _clientAddress = value
        }

    private var _protocolVersion: Int? = null
    var protocolVersion: Int
        get() = checkNotNull(_protocolVersion) { "protocolVersion is not set" }
        set(value) {
            check(_protocolVersion == null)
            _protocolVersion = value
        }

    private var _playerUsername: String? = null
    var playerUsername: String
        get() = checkNotNull(_playerUsername) { "playerUsername is not set" }
        set(value) {
            check(_playerUsername == null)
            _playerUsername = value
        }

    fun <T : ExternalContext> getExternalContext(type: Class<T>): T = type.cast(externalContexts[type])

    fun sendToClient(packet: ByteBuf) {
        attachedS2CPackets.add(packet)
    }

    fun sendToServer(packet: ByteBuf) {
        attachedC2SPackets.add(packet)
    }

    @PublishedApi
    internal fun onConnect() {
        for (externalContext in externalContexts.values) {
            try {
                externalContext.onConnect()
            } catch (exception: Throwable) {
                LOGGER.error("An error occurred while calling onConnect on ${externalContext.javaClass.typeName}", exception)
            }
        }
    }

    @PublishedApi
    internal fun afterLogin() {
        for (externalContext in externalContexts.values) {
            try {
                externalContext.afterLogin()
            } catch (exception: Throwable) {
                LOGGER.error("An error occurred while calling afterLogin on ${externalContext.javaClass.typeName}", exception)
            }
        }
    }

    @PublishedApi
    internal fun onDisconnect() {
        for (externalContext in externalContexts.values) {
            try {
                externalContext.onDisconnect()
            } catch (exception: Throwable) {
                LOGGER.error("An error occurred while calling onDisconnect on ${externalContext.javaClass.typeName}", exception)
            }
        }
    }

    companion object {
        private val LOGGER = LogManager.getLogger(ConnectionContext::class.java)

        private val EXTERNAL_CONTEXT_TYPES: MutableMap<Class<out ExternalContext>, Function<in ConnectionContext, out ExternalContext>> = HashMap()

        @JvmStatic
        fun <T : ExternalContext> registerExternalContextType(type: Class<T>, generator: Supplier<T>) {
            EXTERNAL_CONTEXT_TYPES[type] = Function { generator.get() }
        }

        @JvmStatic
        fun <T : ExternalContext> registerExternalContextType(type: Class<T>, generator: Function<ConnectionContext, T>) {
            EXTERNAL_CONTEXT_TYPES[type] = generator
        }

        @JvmStatic
        fun <T : ExternalContext> unregisterExternalContextType(type: Class<T>) {
            EXTERNAL_CONTEXT_TYPES.remove(type)
        }
    }
}

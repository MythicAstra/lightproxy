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

import java.util.Queue
import java.util.UUID
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.function.Function
import java.util.function.Supplier
import io.netty.buffer.ByteBuf
import net.sharedwonder.lightproxy.addon.ExternalContext
import net.sharedwonder.lightproxy.crypt.EncryptionContext
import net.sharedwonder.lightproxy.crypt.EncryptionDisabledContext
import net.sharedwonder.lightproxy.util.ConnectionState
import org.apache.logging.log4j.LogManager

class ConnectionContext(private val lightProxy: LightProxy) {
    val bindPort: Int get() = lightProxy.bindPort

    val remoteAddress: String get() = lightProxy.remoteAddress

    val remotePort: Int get() = lightProxy.remotePort

    val accounts: AccountMap? get() = lightProxy.accounts

    val attachedC2SPackets: Queue<ByteBuf> = ConcurrentLinkedQueue()

    val attachedS2CPackets: Queue<ByteBuf> = ConcurrentLinkedQueue()

    val externalContexts: Map<Class<out ExternalContext>, ExternalContext> = buildMap {
        for ((type, generator) in externalContextTypes) {
            put(type, generator.apply(this@ConnectionContext))
        }
    }

    var connectionState: ConnectionState = ConnectionState.HANDSHAKE

    var encryptionContext: EncryptionContext = EncryptionDisabledContext()

    var compressionThreshold: Int = -1

    var isEnabledCompressionForClient: Boolean = false

    var isClientFromLocalhost: Boolean = false
        private set

    private var _clientAddress: String? = null
    var clientAddress: String
        get() = checkNotNull(_clientAddress) { "clientAddress is not set" }
        set(value) {
            check(_clientAddress == null)
            _clientAddress = value
            isClientFromLocalhost = value == "127.0.0.1" || value == "::1"
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

    private var _playerUuid: UUID? = null
    var playerUuid: UUID
        get() = checkNotNull(_playerUuid) { "playerUuid is not set" }
        set(value) {
            check(_playerUuid == null)
            _playerUuid = value
        }

    fun <T : ExternalContext> getExternalContext(type: Class<T>): T = type.cast(externalContexts[type])

    fun sendToClient(packet: ByteBuf) {
        attachedS2CPackets.add(packet)
    }

    fun sendToServer(packet: ByteBuf) {
        attachedC2SPackets.add(packet)
    }

    fun onConnect() {
        for (externalContext in externalContexts.values) {
            try {
                externalContext.onConnect()
            } catch (exception: Throwable) {
                logger.error("An error occurred while calling onConnect() on ${externalContext.javaClass.name}", exception)
            }
        }
    }

    fun afterLogin() {
        for (externalContext in externalContexts.values) {
            try {
                externalContext.afterLogin()
            } catch (exception: Throwable) {
                logger.error("An error occurred while calling afterLogin() on ${externalContext.javaClass.name}", exception)
            }
        }
    }

    fun onDisconnect() {
        for (externalContext in externalContexts.values) {
            try {
                externalContext.onDisconnect()
            } catch (exception: Throwable) {
                logger.error("An error occurred while calling onDisconnect() on ${externalContext.javaClass.name}", exception)
            }
        }
    }

    companion object {
        private val logger = LogManager.getLogger(ConnectionContext::class.java)

        private val externalContextTypes: MutableMap<Class<out ExternalContext>, Function<in ConnectionContext, out ExternalContext>> =
            HashMap()

        @JvmStatic
        fun <T : ExternalContext> registerExternalContextType(type: Class<T>, generator: Supplier<T>) {
            externalContextTypes[type] = Function { generator.get() }
        }

        @JvmStatic
        fun <T : ExternalContext> registerExternalContextType(type: Class<T>, generator: Function<in ConnectionContext, T>) {
            externalContextTypes[type] = generator
        }

        @JvmStatic
        fun <T : ExternalContext> unregisterExternalContextType(type: Class<T>) {
            externalContextTypes.remove(type)
        }
    }
}

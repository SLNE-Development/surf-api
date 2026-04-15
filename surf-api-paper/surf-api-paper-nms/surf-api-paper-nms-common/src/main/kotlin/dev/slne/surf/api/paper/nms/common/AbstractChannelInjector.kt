package dev.slne.surf.api.paper.nms.common

import dev.slne.surf.api.core.messages.Colors
import dev.slne.surf.api.core.messages.CommonComponents
import dev.slne.surf.api.core.messages.adventure.text
import dev.slne.surf.api.core.util.logger
import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import io.netty.channel.Channel
import io.netty.channel.ChannelDuplexHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelPromise
import io.netty.util.AttributeKey
import net.kyori.adventure.key.Key
import org.bukkit.entity.Player
import org.bukkit.event.Listener
import org.bukkit.plugin.Plugin
import dev.slne.surf.api.paper.event.register as registerListener
import dev.slne.surf.api.paper.event.unregister as unregisterListener

@OptIn(NmsUseWithCaution::class)
abstract class AbstractChannelInjector<H : AbstractChannelInjector.AbstractPacketHandler> : Listener {
    companion object {
        private val log = logger()

        private val CHANNEL_KEY = Key.key("surf-api", "packet-listener")
        private const val CHANNEL_NAME = "surf_api_packet_listener"

        val instance by lazy { NmsProvider.current.createChannelInjector() }
    }

    private val packetHandlerKey = AttributeKey.newInstance<H>("surf_api_packet_handler")

    /**
     * The plugin instance used for listener registration.
     */
    protected abstract val plugin: Plugin

    fun register() {
        registerChannelInitializeListener(CHANNEL_KEY) { this.getOrInjectPacketHandler(it) }
        registerListener(plugin)
    }

    fun unregister() {
        unregisterListener()
        unregisterChannelInitializeListener(CHANNEL_KEY)
    }

    @Suppress("SameParameterValue")
    protected abstract fun registerChannelInitializeListener(key: Key, listener: (Channel) -> Unit)

    @Suppress("SameParameterValue")
    protected abstract fun unregisterChannelInitializeListener(key: Key)

    protected abstract fun createPacketHandler(): H
    protected abstract fun getNmsChannelHandlerName(): String

    protected fun getOrInjectPacketHandler(channel: Channel): H {
        val handler = createPacketHandler()
        val attr = channel.attr(packetHandlerKey)

        if (!attr.compareAndSet(null, handler)) {
            return attr.get()
        }

        val command = Runnable {
            with(channel.pipeline()) {
                if (get(CHANNEL_NAME) == null) {
                    addBefore(getNmsChannelHandlerName(), CHANNEL_NAME, handler)
                }
            }
        }

        with(channel.eventLoop()) {
            if (inEventLoop()) {
                command.run()
            } else {
                execute(command)
            }
        }

        return handler
    }

    protected fun createFailedToInjectPacketListenerDisconnectReason() = CommonComponents.renderDisconnectMessage(
        "FAILED TO INJECT PACKET LISTENER",
        {
            text(
                "Dein Profil ist nicht authentifiziert, daher können wir den Paket-Listener nicht einbinden. Dies ist wahrscheinlich ein Problem mit deiner Verbindung zu den Authentifizierungsservern von Mojang.",
                Colors.ERROR
            )
        },
        issue = true
    )

    @OptIn(NmsUseWithCaution::class)
    abstract class AbstractPacketHandler : ChannelDuplexHandler() {
        companion object {
            val NO_CONNECTION_SENTINEL = Any()
        }

        private val bridge = InternalNmsBridge.get()
        private val packetListenerApi = InternalPacketListenerApiBridge.get()
        private val packetBridgeHandler = NmsProvider.current.getPacketBridgeHandler()

        @OptIn(NmsUseWithCaution::class)
        override fun write(
            ctx: ChannelHandlerContext,
            msg: Any,
            promise: ChannelPromise,
        ) { // server -> client
            handlePacket(
                originalMsg = msg,
                passthrough = { super.write(ctx, it, promise) },
                nmsHandler = packetListenerApi::handleClientboundPacket,
                bridgeHandler = ::handleClientboundPacketFromBridge
            )
        }

        @OptIn(NmsUseWithCaution::class)
        override fun channelRead(ctx: ChannelHandlerContext, msg: Any) { // client -> server
            handlePacket(
                originalMsg = msg,
                passthrough = { super.channelRead(ctx, it) },
                nmsHandler = packetListenerApi::handleServerboundPacket,
                bridgeHandler = ::handleServerboundPacketFromBridge
            )
        }

        protected abstract fun isPacket(msg: Any): Boolean
        protected abstract fun getPlayerFromConnection(): Any?
        protected abstract fun getBukkitPlayer(player: Any?): Player?

        private inline fun handlePacket(
            originalMsg: Any,
            passthrough: (Any) -> Unit,
            nmsHandler: (packet: Any, serverPlayer: Any?) -> Any?,
            bridgeHandler: (player: Player?, packet: Any) -> Any?
        ) {
            var msg = originalMsg
            if (!isPacket(msg)) {
                passthrough(msg)
                return
            }

            val player = getPlayerFromConnection()
            if (player === NO_CONNECTION_SENTINEL) {
                passthrough(msg)
                return
            }

            try {
                msg = nmsHandler(msg, player) ?: return
                msg = bridgeHandler(getBukkitPlayer(player), msg) ?: return
                passthrough(msg)
            } catch (outOfMemoryError: OutOfMemoryError) {
                throw outOfMemoryError
            } catch (t: Throwable) {
                log.atSevere()
                    .withCause(t).log("Failed to handle packet")
                passthrough(msg)
            }
        }

        @OptIn(NmsUseWithCaution::class)
        fun handleServerboundPacketFromBridge(
            player: Player?,
            packet: Any,
        ): Any? {
            val apiPacket = packetBridgeHandler.wrapServerboundPacket(packet)

            if (apiPacket != null) { // we have an api packet wrapper for this packet
                val resultApi = this.bridge.handleServerboundPacket(
                    apiPacket,
                    player
                )

                if (resultApi != null) { // we may have a modified packet
                    return packetBridgeHandler.unwrapPacket(resultApi)
                }
            } else {
                return packet // no api packet wrapper, so we just return the original packet
            }

            return null // the packet was canceled
        }

        fun handleClientboundPacketFromBridge(
            player: Player?,
            packet: Any,
        ): Any? {
            val apiPacket = packetBridgeHandler.wrapClientboundPacket(packet)

            if (apiPacket != null) {
                val resultApi = this.bridge.handleClientboundPacket(
                    apiPacket,
                    player
                )

                if (resultApi != null) {
                    return packetBridgeHandler.unwrapPacket(resultApi)
                }
            } else {
                return packet
            }

            return null
        }
    }
}


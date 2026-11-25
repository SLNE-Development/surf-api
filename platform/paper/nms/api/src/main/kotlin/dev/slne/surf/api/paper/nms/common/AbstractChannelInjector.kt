package dev.slne.surf.api.paper.nms.common

import dev.slne.surf.api.common.util.logger
import dev.slne.surf.api.messages.CommonComponents
import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.shared.api.util.InternalSurfApi
import dev.slne.surf.api.spring.surfBean
import io.netty.channel.Channel
import io.netty.channel.ChannelDuplexHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelPromise
import io.netty.util.AttributeKey
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.TextComponent
import org.bukkit.entity.Player
import org.bukkit.event.Listener
import org.bukkit.plugin.Plugin
import dev.slne.surf.api.paper.event.register as registerListener
import dev.slne.surf.api.paper.event.unregister as unregisterListener

@OptIn(NmsUseWithCaution::class)
abstract class AbstractChannelInjector<H : AbstractChannelInjector.AbstractPacketHandler> :
    Listener {
    companion object {
        private val log = logger()

        private val CHANNEL_KEY = Key.key("surf-api", "packet-listener")
        private const val CHANNEL_NAME = "surf_api_packet_listener"

        @InternalSurfApi
        val instance by surfBean<AbstractChannelInjector<*>>()
    }

    private val packetHandlerKey = AttributeKey.newInstance<H>("surf_api_packet_handler")

    /**
     * Injects the packet handler into every new channel and registers this listener for [plugin].
     */
    fun register(plugin: Plugin) {
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

    protected fun createFailedToInjectPacketListenerDisconnectReason(): TextComponent {
        return CommonComponents.renderDisconnectMessage(
            disconnectReason = "FAILED TO INJECT PACKET LISTENER",
            suggestHelp = {
                error(
                    "Dein Profil ist nicht authentifiziert, daher können wir den Paket-Listener " +
                        "nicht einbinden. Dies ist wahrscheinlich ein Problem mit deiner Verbindung " +
                        "zu den Authentifizierungsservern von Mojang.",
                )
            },
            issue = true,
        )
    }

    @OptIn(NmsUseWithCaution::class)
    abstract class AbstractPacketHandler(
        private val packetBridgeHandler: NmsPacketBridgeHandler,
        private val bridge: InternalNmsBridge,
        private val packetListenerApi: InternalPacketListenerApiBridge,
    ) : ChannelDuplexHandler() {
        companion object {
            val NO_CONNECTION_SENTINEL = Any()
        }

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
                bridgeHandler = ::handleClientboundPacketFromBridge,
            )
        }

        @OptIn(NmsUseWithCaution::class)
        override fun channelRead(ctx: ChannelHandlerContext, msg: Any) { // client -> server
            handlePacket(
                originalMsg = msg,
                passthrough = { super.channelRead(ctx, it) },
                nmsHandler = packetListenerApi::handleServerboundPacket,
                bridgeHandler = ::handleServerboundPacketFromBridge,
            )
        }

        protected abstract fun isPacket(msg: Any): Boolean
        protected abstract fun getPlayerFromConnection(): Any?
        protected abstract fun getBukkitPlayer(player: Any?): Player?

        private inline fun handlePacket(
            originalMsg: Any,
            passthrough: (Any) -> Unit,
            nmsHandler: (packet: Any, serverPlayer: Any?) -> Any?,
            bridgeHandler: (player: Player?, packet: Any) -> Any?,
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
                    .withCause(t)
                    .log("Failed to handle packet")
                passthrough(msg)
            }
        }

        @OptIn(NmsUseWithCaution::class)
        fun handleServerboundPacketFromBridge(
            player: Player?,
            packet: Any,
        ): Any? {
            val apiPacket = packetBridgeHandler.wrapServerboundPacket(packet)

            if (apiPacket != null) {
                val resultApi = this.bridge.handleServerboundPacket(
                    apiPacket,
                    player,
                )

                if (resultApi != null) {
                    return packetBridgeHandler.unwrapPacket(resultApi)
                }
            } else {
                return packet
            }

            return null
        }

        fun handleClientboundPacketFromBridge(
            player: Player?,
            packet: Any,
        ): Any? {
            val apiPacket = packetBridgeHandler.wrapClientboundPacket(packet)

            if (apiPacket != null) {
                val resultApi = this.bridge.handleClientboundPacket(
                    apiPacket,
                    player,
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


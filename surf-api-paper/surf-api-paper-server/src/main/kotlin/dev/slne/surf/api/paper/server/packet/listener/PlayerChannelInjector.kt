package dev.slne.surf.api.paper.server.packet.listener

import dev.slne.surf.api.core.messages.Colors
import dev.slne.surf.api.core.messages.CommonComponents
import dev.slne.surf.api.core.messages.adventure.text
import dev.slne.surf.api.core.reflection.Field
import dev.slne.surf.api.core.reflection.SurfProxy
import dev.slne.surf.api.core.reflection.SurfReflection
import dev.slne.surf.api.core.reflection.createProxy
import dev.slne.surf.api.core.util.logger
import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.SurfPaperNmsBridge
import dev.slne.surf.api.paper.packet.listener.SurfPaperPacketListenerApi
import dev.slne.surf.api.paper.server.impl.nms.SurfPaperNmsBridgeImpl
import dev.slne.surf.api.paper.server.impl.nms.listener.packets.NmsPacketImpl
import dev.slne.surf.api.paper.server.impl.nms.listener.packets.PacketRegistry
import dev.slne.surf.api.paper.server.impl.packet.listener.SurfPaperPacketListenerApiImpl
import dev.slne.surf.api.paper.server.nms.toNms
import dev.slne.surf.api.paper.server.plugin
import io.netty.channel.Channel
import io.netty.channel.ChannelDuplexHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelPromise
import io.netty.util.AttributeKey
import io.papermc.paper.connection.PaperPlayerLoginConnection
import io.papermc.paper.connection.ReadablePlayerCookieConnectionImpl
import io.papermc.paper.event.connection.PlayerConnectionValidateLoginEvent
import io.papermc.paper.network.ChannelInitializeListenerHolder
import net.kyori.adventure.key.Key
import net.minecraft.network.Connection
import net.minecraft.network.HandlerNames
import net.minecraft.network.protocol.Packet
import net.minecraft.server.level.ServerPlayer
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import dev.slne.surf.api.paper.event.register as registerListener
import dev.slne.surf.api.paper.event.unregister as unregisterListener

@Suppress("UnstableApiUsage")
object PlayerChannelInjector : Listener {
    private val log = logger()

    private val CHANNEL_KEY = Key.key("surf-api", "packet-listener")
    private const val CHANNEL_NAME = "surf_api_packet_listener"

    private val packetHandlerKey =
        AttributeKey.newInstance<PacketHandler>("surf_api_packet_handler")

    fun register() {
        ChannelInitializeListenerHolder.addListener(CHANNEL_KEY) { this.getOrInjectPacketHandler(it) }
        registerListener(plugin)
    }

    fun unregister() {
        unregisterListener()
        ChannelInitializeListenerHolder.removeListener(CHANNEL_KEY)
    }

    private fun getOrInjectPacketHandler(channel: Channel): PacketHandler {
        val handler = PacketHandler()
        val attr = channel.attr(packetHandlerKey)

        if (!attr.compareAndSet(null, handler)) {
            return attr.get()
        }

        val command = Runnable {
            if (channel.pipeline().get(CHANNEL_NAME) == null) {
                channel.pipeline().addBefore(HandlerNames.PACKET_HANDLER, CHANNEL_NAME, handler)
            }
        }

        if (channel.eventLoop().inEventLoop()) {
            command.run()
        } else {
            channel.eventLoop().execute(command)
        }

        return handler
    }

    @EventHandler
    fun onPlayerLogin(event: PlayerConnectionValidateLoginEvent) {
        val paperConnection = event.connection
        if (paperConnection is PaperPlayerLoginConnection) {
            val profile = paperConnection.authenticatedProfile

            if (profile == null) {
                event.kickMessage(
                    CommonComponents.renderDisconnectMessage(
                        "FAILED TO INJECT PACKET LISTENER",
                        {
                            text(
                                "Dein Profil ist nicht authentifiziert, daher können wir den Paket-Listener nicht einbinden. Dies ist wahrscheinlich ein Problem mit deiner Verbindung zu den Authentifizierungsservern von Mojang.",
                                Colors.ERROR
                            )
                        },
                        issue = true
                    )
                )

                log.atWarning()
                    .log("Failed to inject packet listener for player (${paperConnection.unsafeProfile}) with unauthenticated profile: ${paperConnection.address} - ${paperConnection.clientAddress}")
                return
            }

            val connection =
                ReadablePlayerCookieConnectionImplProxy.instance.getConnection(paperConnection)
            val channel = connection.channel
            getOrInjectPacketHandler(channel).connection = connection
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player.toNms()
        val connection = player.connection.connection
        val channel = connection.channel

        getOrInjectPacketHandler(channel).connection = connection
    }

    @OptIn(NmsUseWithCaution::class)
    private class PacketHandler : ChannelDuplexHandler() {
        private val bridge = SurfPaperNmsBridge.INSTANCE as SurfPaperNmsBridgeImpl
        private val packetListenerApi =
            SurfPaperPacketListenerApi.INSTANCE as SurfPaperPacketListenerApiImpl

        @Volatile
        var connection: Connection? = null

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

        private inline fun handlePacket(
            originalMsg: Any,
            passthrough: (Any) -> Unit,
            nmsHandler: (Packet<*>, ServerPlayer?) -> Packet<*>?,
            bridgeHandler: (ServerPlayer?, Packet<*>) -> Packet<*>?
        ) {
            var msg = originalMsg
            if (msg !is Packet<*>) {
                passthrough(msg)
                return
            }

            val connection = connection
            if (connection == null) {
                passthrough(msg)
                return
            }

            val player = connection.player as ServerPlayer?

            try {
                msg = nmsHandler(msg, player) ?: return
                msg = bridgeHandler(player, msg) ?: return
                passthrough(msg)
            } catch (outOfMemoryError: OutOfMemoryError) {
                throw outOfMemoryError
            } catch (t: Throwable) {
                log.atSevere().withCause(t).log("Failed to handle packet")
                passthrough(msg)
            }
        }

        @OptIn(NmsUseWithCaution::class)
        fun handleServerboundPacketFromBridge(
            serverPlayer: ServerPlayer?,
            packet: Packet<*>,
        ): Packet<*>? {
            val apiPacket = PacketRegistry.createServerboundPacketOrNull(packet)

            if (apiPacket != null) { // we have an api packet wrapper for this packet
                val resultApi = this.bridge.handleServerboundPacket(
                    apiPacket,
                    serverPlayer?.bukkitEntity
                )

                if (resultApi != null) { // we may have a modified packet
                    return NmsPacketImpl.getFromApi(resultApi).nmsPacket
                }
            } else {
                return packet // no api packet wrapper, so we just return the original packet
            }

            return null // the packet was canceled
        }

        fun handleClientboundPacketFromBridge(
            serverPlayer: ServerPlayer?,
            packet: Packet<*>,
        ): Packet<*>? {
            val apiPacket = PacketRegistry.createClientboundPacketOrNull(packet)

            if (apiPacket != null) {
                val resultApi = this.bridge.handleClientboundPacket(
                    apiPacket,
                    serverPlayer?.bukkitEntity
                )

                if (resultApi != null) {
                    return NmsPacketImpl.getFromApi(resultApi).nmsPacket
                }
            } else {
                return packet
            }

            return null
        }
    }

    @SurfProxy(ReadablePlayerCookieConnectionImpl::class)
    interface ReadablePlayerCookieConnectionImplProxy {

        @Field("connection", Field.Type.GETTER)
        fun getConnection(instance: ReadablePlayerCookieConnectionImpl): Connection

        companion object {
            val instance = SurfReflection.createProxy<ReadablePlayerCookieConnectionImplProxy>()
        }
    }
}

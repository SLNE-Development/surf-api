package dev.slne.surf.api.paper.server.nms.v26_3.packet.listener

import dev.slne.surf.api.common.util.logger
import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.common.*
import dev.slne.surf.api.paper.server.nms.v26_3.extensions.toNms
import dev.slne.surf.api.reflection.ReflectedField
import dev.slne.surf.api.reflection.ReflectionProxy
import dev.slne.surf.api.reflection.reflectionProxy
import io.netty.channel.Channel
import io.papermc.paper.connection.PaperPlayerLoginConnection
import io.papermc.paper.connection.ReadablePlayerCookieConnectionImpl
import io.papermc.paper.event.connection.PlayerConnectionValidateLoginEvent
import io.papermc.paper.network.ChannelInitializeListenerHolder
import net.kyori.adventure.key.Key
import net.minecraft.network.Connection
import net.minecraft.network.HandlerNames
import net.minecraft.network.protocol.Packet
import net.minecraft.server.level.ServerPlayer
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerJoinEvent

@Suppress("ClassName", "UnstableApiUsage")
@OptIn(NmsUseWithCaution::class)
@NmsComponent
class V26_3ChannelInjector(
    private val packetBridgeHandler: NmsPacketBridgeHandler,
    private val nmsBridge: InternalNmsBridge,
    private val packetListenerApi: InternalPacketListenerApiBridge,
) : AbstractChannelInjector<V26_3ChannelInjector.PacketHandler>() {

    override fun registerChannelInitializeListener(
        key: Key,
        listener: (Channel) -> Unit,
    ) {
        ChannelInitializeListenerHolder.addListener(key, listener)
    }

    override fun unregisterChannelInitializeListener(key: Key) {
        ChannelInitializeListenerHolder.removeListener(key)
    }

    override fun createPacketHandler(): PacketHandler {
        return PacketHandler(packetBridgeHandler, nmsBridge, packetListenerApi)
    }

    override fun getNmsChannelHandlerName(): String = HandlerNames.PACKET_HANDLER

    @EventHandler
    fun onPlayerLogin(event: PlayerConnectionValidateLoginEvent) {
        val paperConnection = event.connection

        if (paperConnection is PaperPlayerLoginConnection) {
            val profile = paperConnection.authenticatedProfile

            if (profile == null) {
                event.kickMessage(createFailedToInjectPacketListenerDisconnectReason())

                log.atWarning()
                    .log(
                        "Failed to inject packet listener for player (%s) with unauthenticated " +
                            "profile: %s - %s",
                        paperConnection.unsafeProfile,
                        paperConnection.address,
                        paperConnection.clientAddress,
                    )

                return
            }

            val cookieConnections = ReadablePlayerCookieConnectionImplProxy.instance
            val connection = cookieConnections.getConnection(paperConnection)
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

    class PacketHandler(
        packetBridgeHandler: NmsPacketBridgeHandler,
        nmsBridge: InternalNmsBridge,
        packetListenerApi: InternalPacketListenerApiBridge,
    ) : AbstractPacketHandler(packetBridgeHandler, nmsBridge, packetListenerApi) {
        @Volatile
        var connection: Connection? = null

        override fun isPacket(msg: Any): Boolean = msg is Packet<*>
        override fun getPlayerFromConnection(): Any? {
            val connection = this.connection ?: return NO_CONNECTION_SENTINEL

            return connection.player as ServerPlayer?
        }

        override fun getBukkitPlayer(player: Any?): Player? {
            return (player as? ServerPlayer)?.bukkitEntity
        }
    }

    private companion object {
        private val log = logger()
    }

    @ReflectionProxy(ReadablePlayerCookieConnectionImpl::class)
    interface ReadablePlayerCookieConnectionImplProxy {

        @ReflectedField("connection")
        fun getConnection(instance: ReadablePlayerCookieConnectionImpl): Connection

        companion object {
            val instance = reflectionProxy<ReadablePlayerCookieConnectionImplProxy>()
        }
    }
}

package dev.slne.surf.api.paper.server.nms.v26_2.packet.listener

import dev.slne.surf.api.core.reflection.Field
import dev.slne.surf.api.core.reflection.SurfProxy
import dev.slne.surf.api.core.reflection.SurfReflection
import dev.slne.surf.api.core.reflection.createProxy
import dev.slne.surf.api.core.util.logger
import dev.slne.surf.api.paper.nms.common.AbstractChannelInjector
import dev.slne.surf.api.paper.server.nms.v26_1.extensions.toNms
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
object V26_2ChannelInjector : AbstractChannelInjector<V26_2ChannelInjector.PacketHandler>() {
    private val log = logger()

    override fun registerChannelInitializeListener(
        key: Key,
        listener: (Channel) -> Unit
    ) {
        ChannelInitializeListenerHolder.addListener(key, listener)
    }

    override fun unregisterChannelInitializeListener(key: Key) {
        ChannelInitializeListenerHolder.removeListener(key)
    }

    override fun createPacketHandler() = PacketHandler()
    override fun getNmsChannelHandlerName(): String = HandlerNames.PACKET_HANDLER

    @EventHandler
    fun onPlayerLogin(event: PlayerConnectionValidateLoginEvent) {
        val paperConnection = event.connection
        if (paperConnection is PaperPlayerLoginConnection) {
            val profile = paperConnection.authenticatedProfile

            if (profile == null) {
                event.kickMessage(createFailedToInjectPacketListenerDisconnectReason())

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

    class PacketHandler : AbstractPacketHandler() {
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

    @SurfProxy(ReadablePlayerCookieConnectionImpl::class)
    interface ReadablePlayerCookieConnectionImplProxy {

        @Field("connection", Field.Type.GETTER)
        fun getConnection(instance: ReadablePlayerCookieConnectionImpl): Connection

        companion object {
            val instance = SurfReflection.createProxy<ReadablePlayerCookieConnectionImplProxy>()
        }
    }
}
package dev.slne.surf.surfapi.bukkit.server.packet.listener

import com.github.benmanes.caffeine.cache.Caffeine
import com.sksamuel.aedile.core.expireAfterAccess
import dev.slne.surf.surfapi.bukkit.api.extensions.server
import dev.slne.surf.surfapi.bukkit.api.nms.NmsUseWithCaution
import dev.slne.surf.surfapi.bukkit.api.nms.nmsBridge
import dev.slne.surf.surfapi.bukkit.server.impl.nms.SurfBukkitNmsBridgeImpl
import dev.slne.surf.surfapi.bukkit.server.impl.nms.listener.packets.NmsPacketImpl
import dev.slne.surf.surfapi.bukkit.server.impl.nms.listener.packets.PacketRegistry
import dev.slne.surf.surfapi.bukkit.server.impl.packet.listener.SurfBukkitPacketListenerApiImpl
import dev.slne.surf.surfapi.bukkit.server.nms.toNms
import dev.slne.surf.surfapi.bukkit.server.plugin
import dev.slne.surf.surfapi.core.api.reflection.Field
import dev.slne.surf.surfapi.core.api.reflection.SurfProxy
import dev.slne.surf.surfapi.core.api.reflection.createProxy
import dev.slne.surf.surfapi.core.api.reflection.surfReflection
import dev.slne.surf.surfapi.core.api.util.logger
import dev.slne.surf.surfapi.core.api.util.synchronize
import io.netty.channel.Channel
import io.netty.channel.ChannelDuplexHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelPromise
import io.papermc.paper.connection.PaperPlayerLoginConnection
import io.papermc.paper.connection.ReadablePlayerCookieConnectionImpl
import io.papermc.paper.event.connection.PlayerConnectionValidateLoginEvent
import io.papermc.paper.network.ChannelInitializeListenerHolder
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import net.kyori.adventure.key.Key
import net.minecraft.network.Connection
import net.minecraft.network.HandlerNames
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.login.ClientboundLoginFinishedPacket
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import java.util.*
import java.util.function.Predicate
import kotlin.time.Duration.Companion.minutes
import dev.slne.surf.surfapi.bukkit.api.event.register as registerListener
import dev.slne.surf.surfapi.bukkit.api.event.unregister as unregisterListener

object PlayerChannelInjector : Listener {
    private val log = logger()

    private val CHANNEL_KEY = Key.key("surf-api", "packet-listener")
    private const val CHANNEL_NAME = "surf_api_packet_listener"

    private val playerInjectorCache = Caffeine.newBuilder()
        .weakValues()
        .expireAfterAccess(1.minutes)
        .build<UUID, Connection>()

    private val injectedChannels = TempObjectSet<Channel>().synchronize()

    private class TempObjectSet<T>() : ObjectOpenHashSet<T>() {
        private var added: Long = 0
        private var removed: Long = 0

        private enum class Operation {
            ADD, REMOVE, REMOVE_ALL, REMOVE_IF, ADD_ALL
        }

        fun printStats(operation: Operation) {
            log.atInfo()
                .log("O:$operation,A:$added,R:$removed,D:${added - removed},S:$size,P:${server.onlinePlayers.size}")
        }

        override fun addAll(c: Collection<T?>): Boolean {
            added += c.size
            printStats(Operation.ADD_ALL)

            return super.addAll(c)
        }

        override fun removeAll(c: Collection<T?>): Boolean {
            removed += c.size
            printStats(Operation.REMOVE_ALL)

            return super.removeAll(c)
        }

        override fun add(element: T?): Boolean {
            added += 1
            printStats(Operation.ADD)

            return super.add(element)
        }

        override fun remove(element: T?): Boolean {
            removed += 1
            printStats(Operation.REMOVE)

            return super.remove(element)
        }

        override fun removeIf(filter: Predicate<in T>): Boolean {
            val removedElements = this.filter(filter::test)

            removed += removedElements.size
            printStats(Operation.REMOVE_IF)

            return super.removeIf(filter)
        }

        companion object {
            private const val serialVersionUID: Long = -2618969176232686100L
        }
    }

    fun register() {
        ChannelInitializeListenerHolder.addListener(CHANNEL_KEY) { this.injectChannel(it) }
        registerListener(plugin)
    }

    fun unregister() {
        ChannelInitializeListenerHolder.removeListener(CHANNEL_KEY)
        unregisterListener()
    }

    private fun injectChannel(channel: Channel): PacketHandler {
        val channelHandler = PacketHandler()

        channel.eventLoop().submit {
            if (injectedChannels.add(channel)) {
                val pipeline = channel.pipeline()

                if (pipeline.get(CHANNEL_NAME) != null) {
                    return@submit
                }

                pipeline.addBefore(HandlerNames.PACKET_HANDLER, CHANNEL_NAME, channelHandler)
            }
        }

        return channelHandler
    }

    @EventHandler
    fun onPlayerLogin(event: PlayerConnectionValidateLoginEvent) {
        val paperConnection = event.connection
        if (paperConnection is PaperPlayerLoginConnection) {
            val profile =
                paperConnection.authenticatedProfile ?: error("Authenticated profile is null")
            val connection =
                ReadablePlayerCookieConnectionImplProxy.instance.getConnection(paperConnection)
            playerInjectorCache.put(
                profile.id ?: error("PlayerProfile does not provide a uuid"),
                connection
            )
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player.toNms()
        val connection = player.connection.connection
        val channelHandler = connection.channel.pipeline().get(CHANNEL_NAME)

        if (channelHandler != null) {
            if (channelHandler is PacketHandler) {
                channelHandler.connection = connection
                playerInjectorCache.invalidate(player.uuid)
            }
            return
        }

        injectChannel(connection.channel).connection = connection
    }

    private class PacketHandler : ChannelDuplexHandler() {
        @Volatile
        var connection: Connection? = null

        override fun channelUnregistered(ctx: ChannelHandlerContext) {
            injectedChannels.remove(ctx.channel())
            super.channelUnregistered(ctx)
        }

        @OptIn(NmsUseWithCaution::class)
        override fun write(
            ctx: ChannelHandlerContext?,
            msg: Any?,
            promise: ChannelPromise?,
        ) { // server -> client

            var msg = msg
            if (msg !is Packet<*>) { // not our packet
                super.write(ctx, msg, promise)
                return
            }

            if (connection == null && msg is ClientboundLoginFinishedPacket) {
                val uuid = msg.gameProfile().id
                val cachedConnection = playerInjectorCache.getIfPresent(uuid)
                if (cachedConnection != null) {
                    connection = cachedConnection
                }
            }

            val connection = connection
            val player = connection?.player
            if (connection == null || player == null) {
                super.write(ctx, msg, promise)
                return
            }

            var cancelled = false

            try {
                // first, we try to handle the packet with the nms packet listener
                msg = this.packetListenerApi.handleClientboundPacket(msg, connection.player)

                if (msg == null) {
                    // no need to handle the packet further
                    cancelled = true
                } else {
                    // then we try to handle the packet with the api packet listener
                    msg = handleClientboundPacketFromBridge(connection, msg)
                    cancelled = (msg == null)
                }
            } catch (error: OutOfMemoryError) {
                throw error
            } catch (t: Throwable) {
                log.atSevere()
                    .withCause(t)
                    .log("Failed to handle clientbound packet")
                super.write(ctx, msg, promise)
            }

            if (!cancelled) {
                super.write(ctx, msg, promise)
            }
        }

        @OptIn(NmsUseWithCaution::class)
        override fun channelRead(ctx: ChannelHandlerContext?, msg: Any?) { // client -> server
            var msg = msg
            if (msg !is Packet<*>) { // not our packet
                super.channelRead(ctx, msg)
                return
            }

            val connection = connection
            val player = connection?.player
            if (connection == null || player == null) {
                super.channelRead(ctx, msg)
                return
            }

            var cancelled = false

            try {
                // first, we try to handle the packet with the nms packet listener
                msg = this.packetListenerApi.handleServerboundPacket(msg, connection.player)

                if (msg == null) {
                    // no need to handle the packet further
                    cancelled = true
                } else {
                    // then we try to handle the packet with the api packet listener
                    msg = handleServerboundPacketFromBridge(connection, msg)
                    cancelled = (msg == null)
                }
            } catch (error: OutOfMemoryError) {
                throw error
            } catch (t: Throwable) {
                log.atSevere()
                    .withCause(t)
                    .log("Failed to handle serverbound packet")
                super.channelRead(ctx, msg)
            }

            if (!cancelled) {
                super.channelRead(ctx, msg)
            }
        }

        @OptIn(NmsUseWithCaution::class)
        fun handleServerboundPacketFromBridge(
            connection: Connection,
            packet: Packet<*>,
        ): Packet<*>? {
            val apiPacket = PacketRegistry.createServerboundPacketOrNull(packet)

            if (apiPacket != null) { // we have an api packet wrapper for this packet
                val resultApi = this.bridge.handleServerboundPacket(
                    apiPacket,
                    connection.player.bukkitEntity
                )

                if (resultApi != null) { // we may have a modified packet
                    return NmsPacketImpl.getFromApi(resultApi).nmsPacket
                }
            } else {
                return packet // no api packet wrapper, so we just return the original packet
            }

            return null // the packet was canceled
        }

        @OptIn(NmsUseWithCaution::class)
        fun handleClientboundPacketFromBridge(
            connection: Connection,
            packet: Packet<*>,
        ): Packet<*>? {
            val apiPacket = PacketRegistry.createClientboundPacketOrNull(packet)

            if (apiPacket != null) {
                val resultApi = this.bridge.handleClientboundPacket(
                    apiPacket,
                    connection.player.bukkitEntity
                )

                if (resultApi != null) {
                    return NmsPacketImpl.getFromApi(resultApi).nmsPacket
                }
            } else {
                return packet
            }

            return null
        }

        @OptIn(NmsUseWithCaution::class)
        val bridge get() = nmsBridge as SurfBukkitNmsBridgeImpl

        @OptIn(NmsUseWithCaution::class)
        val packetListenerApi get() = dev.slne.surf.surfapi.bukkit.api.packet.listener.packetListenerApi as SurfBukkitPacketListenerApiImpl
    }

    @SurfProxy(ReadablePlayerCookieConnectionImpl::class)
    interface ReadablePlayerCookieConnectionImplProxy {

        @Field("connection", Field.Type.GETTER)
        fun getConnection(instance: ReadablePlayerCookieConnectionImpl): Connection

        companion object {
            val instance = surfReflection.createProxy<ReadablePlayerCookieConnectionImplProxy>()
        }
    }
}

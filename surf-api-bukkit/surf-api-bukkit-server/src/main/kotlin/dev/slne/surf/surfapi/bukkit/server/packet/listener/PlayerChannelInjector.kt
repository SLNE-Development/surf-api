package dev.slne.surf.surfapi.bukkit.server.packet.listener

import dev.slne.surf.surfapi.bukkit.api.nms.NmsUseWithCaution
import dev.slne.surf.surfapi.bukkit.api.nms.nmsBridge
import dev.slne.surf.surfapi.bukkit.server.impl.nms.SurfBukkitNmsBridgeImpl
import dev.slne.surf.surfapi.bukkit.server.impl.nms.listener.packets.NmsPacketImpl
import dev.slne.surf.surfapi.bukkit.server.impl.nms.listener.packets.PacketRegistry
import dev.slne.surf.surfapi.bukkit.server.impl.packet.listener.SurfBukkitPacketListenerApiImpl
import dev.slne.surf.surfapi.bukkit.server.nms.toNms
import dev.slne.surf.surfapi.bukkit.server.plugin
import dev.slne.surf.surfapi.core.api.util.logger
import dev.slne.surf.surfapi.core.api.util.mutableObject2ObjectMapOf
import dev.slne.surf.surfapi.core.api.util.mutableObjectSetOf
import dev.slne.surf.surfapi.core.api.util.synchronize
import io.netty.channel.Channel
import io.netty.channel.ChannelDuplexHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelPromise
import io.papermc.paper.network.ChannelInitializeListenerHolder
import net.kyori.adventure.key.Key
import net.minecraft.network.HandlerNames
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.login.ClientboundLoginFinishedPacket
import net.minecraft.server.level.ServerPlayer
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerLoginEvent
import java.util.*
import dev.slne.surf.surfapi.bukkit.api.event.register as registerListener
import dev.slne.surf.surfapi.bukkit.api.event.unregister as unregisterListener

object PlayerChannelInjector : Listener {
    private val log = logger()

    private val CHANNEL_KEY = Key.key("surf-api", "packet-listener")
    private const val CHANNEL_NAME = "surf_api_packet_listener"

    private val playerInjectorCache = mutableObject2ObjectMapOf<UUID, ServerPlayer>().synchronize()
    private val injectedChannels = mutableObjectSetOf<Channel>().synchronize()

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

    @EventHandler(priority = EventPriority.LOWEST)
    fun onPlayerLogin(event: PlayerLoginEvent) {
        val player = event.player.toNms()
        playerInjectorCache.put(player.getUUID(), player)
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player.toNms()

        val channel = player.connection.connection.channel
        val channelHandler = channel.pipeline().get(CHANNEL_NAME)

        if (channelHandler != null) {
            if (channelHandler is PacketHandler) {
                channelHandler.player = player // Just in case the player is not set yet
                playerInjectorCache.remove(player.getUUID())
            }

            return
        }

        injectChannel(channel).player = player
    }

    private class PacketHandler : ChannelDuplexHandler() {
        @Volatile
        var player: ServerPlayer? = null

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

            // first, we set the player if it isn't set yet
            if (player == null && msg is ClientboundLoginFinishedPacket) {
                val uuid = msg.gameProfile().id
                val player = playerInjectorCache.remove(uuid)

                if (player != null) {
                    this.player = player
                }
            }

            var cancelled = false

            try {
                // first, we try to handle the packet with the nms packet listener
                msg = this.packetListenerApi.handleClientboundPacket(msg, player!!)

                if (msg == null) {
                    // no need to handle the packet further
                    cancelled = true
                } else {
                    // then we try to handle the packet with the api packet listener
                    msg = handleClientboundPacketFromBridge(msg)
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

            var cancelled = false

            try {
                // first, we try to handle the packet with the nms packet listener
                msg = this.packetListenerApi.handleServerboundPacket(msg, player!!)

                if (msg == null) {
                    // no need to handle the packet further
                    cancelled = true
                } else {
                    // then we try to handle the packet with the api packet listener
                    msg = handleServerboundPacketFromBridge(msg)
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
        fun handleServerboundPacketFromBridge(packet: Packet<*>): Packet<*>? {
            val apiPacket = PacketRegistry.createServerboundPacketOrNull(packet)

            if (apiPacket != null) { // we have an api packet wrapper for this packet
                val resultApi = this.bridge.handleServerboundPacket(
                    apiPacket,
                    player!!.bukkitEntity
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
        fun handleClientboundPacketFromBridge(packet: Packet<*>): Packet<*>? {
            val apiPacket = PacketRegistry.createClientboundPacketOrNull(packet)

            if (apiPacket != null) {
                val resultApi = this.bridge.handleClientboundPacket(
                    apiPacket,
                    player!!.bukkitEntity
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
}

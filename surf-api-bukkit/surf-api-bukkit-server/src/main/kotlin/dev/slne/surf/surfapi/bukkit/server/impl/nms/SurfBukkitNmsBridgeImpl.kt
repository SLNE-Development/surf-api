package dev.slne.surf.surfapi.bukkit.server.impl.nms

import com.google.auto.service.AutoService
import com.google.common.flogger.StackSize
import dev.slne.surf.surfapi.bukkit.api.nms.NmsUseWithCaution
import dev.slne.surf.surfapi.bukkit.api.nms.SurfBukkitNmsBridge
import dev.slne.surf.surfapi.bukkit.api.nms.listener.NmsClientboundPacketListener
import dev.slne.surf.surfapi.bukkit.api.nms.listener.NmsServerboundPacketListener
import dev.slne.surf.surfapi.bukkit.api.nms.listener.packets.clientbound.NmsClientboundPacket
import dev.slne.surf.surfapi.bukkit.api.nms.listener.packets.serverbound.NmsServerboundPacket
import dev.slne.surf.surfapi.bukkit.api.packet.listener.listener.PacketListenerResult
import dev.slne.surf.surfapi.core.api.util.checkInstantiationByServiceLoader
import dev.slne.surf.surfapi.core.api.util.logger
import org.bukkit.entity.Player
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet

@AutoService(SurfBukkitNmsBridge::class)
@NmsUseWithCaution
class SurfBukkitNmsBridgeImpl : SurfBukkitNmsBridge {
    private val log = logger()

    private val serverboundPacketListeners =
        ConcurrentHashMap<Class<*>, CopyOnWriteArraySet<NmsServerboundPacketListener<*>>>()
    private val clientboundPacketListeners =
        ConcurrentHashMap<Class<*>, CopyOnWriteArraySet<NmsClientboundPacketListener<*>>>()

    init {
        checkInstantiationByServiceLoader()
    }

    override fun registerServerboundPacketListener(listener: NmsServerboundPacketListener<*>) {
        val packetClass = listener.packetClass
        val added =
            serverboundPacketListeners.computeIfAbsent(packetClass) { CopyOnWriteArraySet() }.add(listener)

        if (!added) {
            log.atWarning()
                .withStackTrace(StackSize.MEDIUM)
                .log("Serverbound packet listener $listener is already registered")
        }
    }

    override fun unregisterServerboundPacketListener(listener: NmsServerboundPacketListener<*>) {
        val removed = serverboundPacketListeners[listener.packetClass]?.remove(listener) == true

        if (!removed) {
            log.atWarning()
                .withStackTrace(StackSize.MEDIUM)
                .log("Serverbound packet listener $listener is not registered")
        }
    }

    override fun registerClientboundPacketListener(listener: NmsClientboundPacketListener<*>) {
        val packetClass = listener.packetClass
        val added =
            clientboundPacketListeners.computeIfAbsent(packetClass) { CopyOnWriteArraySet() }.add(listener)

        if (!added) {
            log.atWarning()
                .withStackTrace(StackSize.MEDIUM)
                .log("Clientbound packet listener $listener is already registered")
        }
    }

    override fun unregisterClientboundPacketListener(listener: NmsClientboundPacketListener<*>) {
        val removed = clientboundPacketListeners[listener.packetClass]?.remove(listener) == true

        if (!removed) {
            log.atWarning()
                .withStackTrace(StackSize.MEDIUM)
                .log("Clientbound packet listener $listener is not registered")
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <Packet : NmsServerboundPacket> handleServerboundPacket(
        packet: Packet,
        player: Player?,
    ): Packet? {
        val clazz = packet.packetClass
        val listener = serverboundPacketListeners[clazz] ?: return packet

        var cancel = false
        for (listener in listener) {
            listener as NmsServerboundPacketListener<Packet>
            val result = try {
                listener.handleEarlyServerboundPacket(packet, player)
            } catch (e: Throwable) {
                log.atSevere()
                    .withCause(e)
                    .log("Failed to handle serverbound packet $clazz for listener $listener")
                PacketListenerResult.CONTINUE
            }

            if (result == PacketListenerResult.CANCEL) {
                cancel = true
            }
        }

        return if (cancel) null else packet
    }

    @Suppress("UNCHECKED_CAST")
    fun <Packet : NmsClientboundPacket> handleClientboundPacket(
        packet: Packet,
        player: Player?,
    ): Packet? {
        val listeners = clientboundPacketListeners[packet.packetClass] ?: return packet

        if (listeners.isEmpty()) return packet

        var cancel = false
        for (listener in listeners) {
            listener as NmsClientboundPacketListener<Packet>
            val result = try {
                listener.handleEarlyClientboundPacket(packet, player)
            } catch (e: Throwable) {
                log.atSevere()
                    .withCause(e)
                    .log("Failed to handle clientbound packet ${packet.packetClass} for listener $listener")
                PacketListenerResult.CONTINUE
            }

            if (result == PacketListenerResult.CANCEL) {
                cancel = true
            }
        }

        return if (cancel) null else packet
    }
}

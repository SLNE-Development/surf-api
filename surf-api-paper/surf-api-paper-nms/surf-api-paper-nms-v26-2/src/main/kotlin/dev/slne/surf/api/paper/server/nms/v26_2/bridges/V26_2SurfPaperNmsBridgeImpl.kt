package dev.slne.surf.api.paper.server.nms.v26_2.bridges

import com.google.common.flogger.StackSize
import dev.slne.surf.api.core.util.logger
import dev.slne.surf.api.paper.api.nms.listener.NmsClientboundPacketListener
import dev.slne.surf.api.paper.api.nms.listener.NmsServerboundPacketListener
import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.common.InternalNmsBridge
import dev.slne.surf.api.paper.nms.listener.packets.clientbound.NmsClientboundPacket
import dev.slne.surf.api.paper.nms.listener.packets.serverbound.NmsServerboundPacket
import dev.slne.surf.api.paper.packet.listener.listener.PacketListenerResult
import org.bukkit.entity.Player
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet

@NmsUseWithCaution
@Suppress("ClassName")
class V26_2SurfPaperNmsBridgeImpl : InternalNmsBridge {
    private typealias PacketListenerMap<T> = ConcurrentHashMap<Class<*>, CopyOnWriteArraySet<T>>

    private val log = logger()

    private val serverboundPacketListeners = PacketListenerMap<NmsServerboundPacketListener<*>>()
    private val clientboundPacketListeners = PacketListenerMap<NmsClientboundPacketListener<*>>()

    override fun registerServerboundPacketListener(listener: NmsServerboundPacketListener<*>) {
        val packetClass = listener.packetClass
        val added =
            serverboundPacketListeners.computeIfAbsent(packetClass) { CopyOnWriteArraySet() }
                .add(listener)

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
            clientboundPacketListeners.computeIfAbsent(packetClass) { CopyOnWriteArraySet() }
                .add(listener)

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
    override fun <Packet : NmsServerboundPacket> handleServerboundPacket(
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
    override fun <Packet : NmsClientboundPacket> handleClientboundPacket(
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

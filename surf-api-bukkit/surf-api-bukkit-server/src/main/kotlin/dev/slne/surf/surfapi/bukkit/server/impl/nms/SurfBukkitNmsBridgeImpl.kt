package dev.slne.surf.surfapi.bukkit.server.impl.nms

import com.google.common.flogger.StackSize
import dev.slne.surf.surfapi.bukkit.api.nms.NmsUseWithCaution
import dev.slne.surf.surfapi.bukkit.api.nms.SurfBukkitNmsBridge
import dev.slne.surf.surfapi.bukkit.api.nms.listener.NmsClientboundPacketListener
import dev.slne.surf.surfapi.bukkit.api.nms.listener.NmsServerboundPacketListener
import dev.slne.surf.surfapi.bukkit.api.nms.listener.packets.clientbound.NmsClientboundPacket
import dev.slne.surf.surfapi.bukkit.api.nms.listener.packets.serverbound.NmsServerboundPacket
import dev.slne.surf.surfapi.bukkit.api.packet.listener.listener.PacketListenerResult
import dev.slne.surf.surfapi.bukkit.server.impl.nms.listener.packets.NmsPacketImpl
import dev.slne.surf.surfapi.core.api.util.*
import org.bukkit.entity.Player

@NmsUseWithCaution
class SurfBukkitNmsBridgeImpl : SurfBukkitNmsBridge {
    private val log = logger()

    private val serverboundPacketListeners =
        mutableObjectSetOf<Pair<SurfTypeParameterMatcher, NmsServerboundPacketListener<*>>>().synchronize()
    private val clientboundPacketListeners =
        mutableObjectSetOf<Pair<SurfTypeParameterMatcher, NmsClientboundPacketListener<*>>>().synchronize()

    init {
        checkInstantiationByServiceLoader()
    }

    override fun registerServerboundPacketListener(listener: NmsServerboundPacketListener<*>) {
        val matcher = listener.packetMatcher
        val added = serverboundPacketListeners.add(matcher to listener)
        if (!added) {
            log.atWarning()
                .withStackTrace(StackSize.MEDIUM)
                .log("Serverbound packet listener $listener is already registered")
        }
    }

    override fun unregisterServerboundPacketListener(listener: NmsServerboundPacketListener<*>) {
        val removed = serverboundPacketListeners.removeIf { it.second == listener }

        if (!removed) {
            log.atWarning()
                .withStackTrace(StackSize.MEDIUM)
                .log("Serverbound packet listener $listener is not registered")
        }
    }

    override fun registerClientboundPacketListener(listener: NmsClientboundPacketListener<*>) {

        val matcher = listener.packetMatcher
        val added = clientboundPacketListeners.add(matcher to listener)
        if (!added) {
            log.atWarning()
                .withStackTrace(StackSize.MEDIUM)
                .log("Clientbound packet listener $listener is already registered")
        }
    }

    override fun unregisterClientboundPacketListener(listener: NmsClientboundPacketListener<*>) {
        val removed = clientboundPacketListeners.removeIf { it.second == listener }

        if (!removed) {
            log.atWarning()
                .withStackTrace(StackSize.MEDIUM)
                .log("Clientbound packet listener $listener is not registered")
        }
    }

    fun <Packet : NmsServerboundPacket> handleServerboundPacket(
        packet: Packet,
        player: Player
    ): Packet? {
        val cancel = serverboundPacketListeners.asSequence()
            .filter { it.first.match(packet) }
            .map { it.second }
            .filterIsInstance<NmsServerboundPacketListener<Packet>>()
            .map { it.handleServerboundPacket(packet, player) }
            .any { it == PacketListenerResult.CANCEL }

        return if (cancel) null else packet
    }

    fun <Packet : NmsClientboundPacket> handleClientboundPacket(
        packet: Packet,
        player: Player
    ): Packet? {
        val listeners = clientboundPacketListeners.asSequence()
            .filter { it.first.match(NmsPacketImpl.getFromApi(packet).nmsClass) }
            .map { it.second }
            .toObjectSet()

        if (listeners.isEmpty()) return packet

        val cancel = clientboundPacketListeners.asSequence()
            .filter { it.first.match(NmsPacketImpl.getFromApi(packet).nmsClass) }
            .map { it.second }
            .filterIsInstance<NmsClientboundPacketListener<Packet>>()
            .map { it.handleClientboundPacket(packet, player) }
            .any { it == PacketListenerResult.CANCEL }

        return if (cancel) null else packet
    }
}

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
import dev.slne.surf.surfapi.bukkit.server.impl.nms.listener.packets.NmsPacketImpl
import dev.slne.surf.surfapi.core.api.util.*
import it.unimi.dsi.fastutil.objects.ObjectSet
import org.bukkit.entity.Player

@AutoService(SurfBukkitNmsBridge::class)
@NmsUseWithCaution
class SurfBukkitNmsBridgeImpl : SurfBukkitNmsBridge {
    private val log = logger()

    private val serverboundPacketListeners =
        mutableObject2ObjectMapOf<Class<*>, ObjectSet<NmsServerboundPacketListener<*>>>().synchronize()
    private val clientboundPacketListeners =
        mutableObject2ObjectMapOf<Class<*>, ObjectSet<NmsClientboundPacketListener<*>>>().synchronize()

    init {
        checkInstantiationByServiceLoader()
    }

    override fun registerServerboundPacketListener(listener: NmsServerboundPacketListener<*>) {
        val packetClass = listener.packetClass
        val added =
            serverboundPacketListeners.computeIfAbsent(packetClass) { mutableObjectSetOf() }.add(listener)


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
            clientboundPacketListeners.computeIfAbsent(packetClass) { mutableObjectSetOf() }.add(listener)

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
        player: Player,
    ): Packet? {
        val clazz = packet.packetClass
        val listener = serverboundPacketListeners[clazz] ?: return packet

        val cancel = listener.asSequence()
            .map { it as NmsServerboundPacketListener<Packet> }
            .map { it.handleServerboundPacket(packet, player) }
            .any { it == PacketListenerResult.CANCEL }

        return if (cancel) null else packet
    }

    @Suppress("UNCHECKED_CAST")
    fun <Packet : NmsClientboundPacket> handleClientboundPacket(
        packet: Packet,
        player: Player,
    ): Packet? {
        val listeners = clientboundPacketListeners[NmsPacketImpl.getFromApi(packet).nmsClass] ?: return packet

        if (listeners.isEmpty()) return packet

        val cancel = listeners.asSequence()
            .map { it as NmsClientboundPacketListener<Packet> }
            .map { it.handleClientboundPacket(packet, player) }
            .any { it == PacketListenerResult.CANCEL }

        return if (cancel) null else packet
    }
}

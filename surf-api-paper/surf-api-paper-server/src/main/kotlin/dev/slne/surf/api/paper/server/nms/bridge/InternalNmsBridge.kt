package dev.slne.surf.api.paper.server.nms.bridge

import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.SurfPaperNmsBridge
import dev.slne.surf.api.paper.nms.listener.packets.clientbound.NmsClientboundPacket
import dev.slne.surf.api.paper.nms.listener.packets.serverbound.NmsServerboundPacket
import org.bukkit.entity.Player

@NmsUseWithCaution
interface InternalNmsBridge : SurfPaperNmsBridge {
    fun <Packet : NmsServerboundPacket> handleServerboundPacket(
        packet: Packet,
        player: Player?,
    ): Packet?

    fun <Packet : NmsClientboundPacket> handleClientboundPacket(
        packet: Packet,
        player: Player?,
    ): Packet?

    companion object {
        fun get() = SurfPaperNmsBridge.INSTANCE as InternalNmsBridge
    }
}
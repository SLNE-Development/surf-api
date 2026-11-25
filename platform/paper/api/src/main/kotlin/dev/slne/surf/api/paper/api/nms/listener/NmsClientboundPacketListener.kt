package dev.slne.surf.api.paper.api.nms.listener

import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.listener.packets.NmsPacket
import dev.slne.surf.api.paper.packet.listener.listener.PacketListenerResult
import org.bukkit.entity.Player
import org.jetbrains.annotations.ApiStatus

/**
 * Receives the packets of type [Packet] that the server sends to a player.
 */
@NmsUseWithCaution
interface NmsClientboundPacketListener<Packet : NmsPacket> : NmsPacketListener<Packet> {

    /**
     * Handles [packet] on its way to [player].
     *
     * @return [PacketListenerResult.CANCEL] to drop the packet.
     */
    @ApiStatus.OverrideOnly
    fun handleClientboundPacket(packet: Packet, player: Player): PacketListenerResult

    /**
     * Handles [packet] that may be sent before the connection belongs to a player.
     *
     * Calls [handleClientboundPacket] when [player] is known and lets the packet pass otherwise.
     */
    @ApiStatus.OverrideOnly
    fun handleEarlyClientboundPacket(packet: Packet, player: Player?): PacketListenerResult {
        if (player == null) return PacketListenerResult.CONTINUE

        return handleClientboundPacket(packet, player)
    }
}

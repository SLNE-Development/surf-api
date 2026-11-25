package dev.slne.surf.api.paper.api.nms.listener

import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.listener.packets.NmsPacket
import dev.slne.surf.api.paper.packet.listener.listener.PacketListenerResult
import org.bukkit.entity.Player
import org.jetbrains.annotations.ApiStatus

/**
 * Receives the packets of type [Packet] that a player sends to the server.
 */
@NmsUseWithCaution
interface NmsServerboundPacketListener<Packet : NmsPacket> : NmsPacketListener<Packet> {

    /**
     * Handles [packet] received from [player].
     *
     * @return [PacketListenerResult.CANCEL] to drop the packet.
     */
    @ApiStatus.OverrideOnly
    fun handleServerboundPacket(packet: Packet, player: Player): PacketListenerResult

    /**
     * Handles [packet] that may arrive before the connection belongs to a player.
     *
     * Calls [handleServerboundPacket] when [player] is known and lets the packet pass otherwise.
     */
    @ApiStatus.OverrideOnly
    fun handleEarlyServerboundPacket(packet: Packet, player: Player?): PacketListenerResult {
        if (player == null) return PacketListenerResult.CONTINUE

        return handleServerboundPacket(packet, player)
    }
}

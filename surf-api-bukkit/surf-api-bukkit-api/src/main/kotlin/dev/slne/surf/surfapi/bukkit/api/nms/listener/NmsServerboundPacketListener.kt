package dev.slne.surf.surfapi.bukkit.api.nms.listener

import dev.slne.surf.surfapi.bukkit.api.nms.NmsUseWithCaution
import dev.slne.surf.surfapi.bukkit.api.nms.listener.packets.serverbound.NmsServerboundPacket
import dev.slne.surf.surfapi.bukkit.api.packet.listener.listener.PacketListenerResult
import org.bukkit.entity.Player
import org.jetbrains.annotations.ApiStatus

@NmsUseWithCaution
interface NmsServerboundPacketListener<Packet : NmsServerboundPacket> : NmsPacketListener<Packet> {
    @ApiStatus.OverrideOnly
    fun handleServerboundPacket(packet: Packet, player: Player): PacketListenerResult
}
package dev.slne.surf.surfapi.bukkit.server.impl.nms.listener.packets.serverbound

import dev.slne.surf.surfapi.bukkit.api.nms.NmsUseWithCaution
import dev.slne.surf.surfapi.bukkit.api.nms.listener.packets.serverbound.ContainerClosePacket
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket

@NmsUseWithCaution
class ContainerClosePacketImpl(
    nmsPacket: ServerboundContainerClosePacket,
) : NmsServerboundPacketImpl<ServerboundContainerClosePacket>(nmsPacket), ContainerClosePacket {
    override val containerId get() = nmsPacket.containerId
}
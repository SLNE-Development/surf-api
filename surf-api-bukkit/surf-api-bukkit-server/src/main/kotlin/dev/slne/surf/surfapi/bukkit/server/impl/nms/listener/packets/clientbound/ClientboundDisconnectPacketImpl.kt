package dev.slne.surf.surfapi.bukkit.server.impl.nms.listener.packets.clientbound

import dev.slne.surf.surfapi.bukkit.api.nms.NmsUseWithCaution
import dev.slne.surf.surfapi.bukkit.api.nms.listener.packets.clientbound.DisconnectPacket
import dev.slne.surf.surfapi.bukkit.server.nms.toBukkit
import net.minecraft.network.protocol.common.ClientboundDisconnectPacket

@NmsUseWithCaution
class ClientboundDisconnectPacketImpl(nmsPacket: ClientboundDisconnectPacket) :
    NmsClientboundPacketImpl<ClientboundDisconnectPacket>(nmsPacket), DisconnectPacket {
    override val reason get() = nmsPacket.reason.toBukkit()
}
package dev.slne.surf.api.paper.server.impl.nms.listener.packets.clientbound

import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.listener.packets.clientbound.DisconnectPacket
import dev.slne.surf.api.paper.server.nms.toBukkit
import net.minecraft.network.protocol.common.ClientCommonPacketListener
import net.minecraft.network.protocol.common.ClientboundDisconnectPacket

@NmsUseWithCaution
class ClientboundDisconnectPacketImpl(nmsPacket: ClientboundDisconnectPacket) :
    NmsClientboundPacketImpl<ClientboundDisconnectPacket, ClientCommonPacketListener>(nmsPacket),
    DisconnectPacket {
    override val reason get() = nmsPacket.reason.toBukkit()
}
package dev.slne.surf.api.paper.server.nms.v1_21_11.listener.packets.clientbound

import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.listener.packets.clientbound.DisconnectPacket
import dev.slne.surf.api.paper.server.nms.v1_21_11.extensions.toBukkit
import net.minecraft.network.protocol.common.ClientCommonPacketListener
import net.minecraft.network.protocol.common.ClientboundDisconnectPacket

@NmsUseWithCaution
class V1_21_11ClientboundDisconnectPacketImpl(nmsPacket: ClientboundDisconnectPacket) :
    V1_21_11NmsClientboundPacketImpl<ClientboundDisconnectPacket, ClientCommonPacketListener>(nmsPacket),
    DisconnectPacket {
    override val reason get() = nmsPacket.reason.toBukkit()
}

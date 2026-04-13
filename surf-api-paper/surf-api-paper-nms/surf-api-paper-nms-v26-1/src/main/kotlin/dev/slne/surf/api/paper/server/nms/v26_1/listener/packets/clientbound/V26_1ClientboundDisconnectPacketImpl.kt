package dev.slne.surf.api.paper.server.nms.v26_1.listener.packets.clientbound

import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.listener.packets.clientbound.DisconnectPacket
import dev.slne.surf.api.paper.server.nms.v26_1.extensions.toBukkit
import net.minecraft.network.protocol.common.ClientCommonPacketListener
import net.minecraft.network.protocol.common.ClientboundDisconnectPacket

@NmsUseWithCaution
class V26_1ClientboundDisconnectPacketImpl(nmsPacket: ClientboundDisconnectPacket) :
    V26_1NmsClientboundPacketImpl<ClientboundDisconnectPacket, ClientCommonPacketListener>(nmsPacket),
    DisconnectPacket {
    override val reason get() = nmsPacket.reason.toBukkit()
}

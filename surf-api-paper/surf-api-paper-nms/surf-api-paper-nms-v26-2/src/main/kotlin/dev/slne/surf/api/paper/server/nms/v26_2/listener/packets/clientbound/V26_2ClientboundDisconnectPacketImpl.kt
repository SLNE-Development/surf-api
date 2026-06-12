package dev.slne.surf.api.paper.server.nms.v26_2.listener.packets.clientbound

import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.listener.packets.clientbound.DisconnectPacket
import dev.slne.surf.api.paper.server.nms.v26_1.extensions.toBukkit
import net.minecraft.network.protocol.common.ClientCommonPacketListener
import net.minecraft.network.protocol.common.ClientboundDisconnectPacket

@NmsUseWithCaution
@Suppress("ClassName")
class V26_2ClientboundDisconnectPacketImpl(nmsPacket: ClientboundDisconnectPacket) :
    V26_2NmsClientboundPacketImpl<ClientboundDisconnectPacket, ClientCommonPacketListener>(nmsPacket),
    DisconnectPacket {
    override val reason get() = nmsPacket.reason.toBukkit()
}

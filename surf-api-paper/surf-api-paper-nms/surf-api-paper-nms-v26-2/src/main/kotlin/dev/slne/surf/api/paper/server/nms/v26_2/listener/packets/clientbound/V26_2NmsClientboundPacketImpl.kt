package dev.slne.surf.api.paper.server.nms.v26_2.listener.packets.clientbound

import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.listener.packets.clientbound.NmsClientboundPacket
import dev.slne.surf.api.paper.server.nms.v26_2.listener.packets.V26_2NmsPacketImpl
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.common.ClientCommonPacketListener

@NmsUseWithCaution
@Suppress("ClassName")
abstract class V26_2NmsClientboundPacketImpl<Nms : Packet<Listener>, Listener : ClientCommonPacketListener>(
    nmsPacket: Nms,
) : NmsClientboundPacket, V26_2NmsPacketImpl<Nms, Listener>(nmsPacket)

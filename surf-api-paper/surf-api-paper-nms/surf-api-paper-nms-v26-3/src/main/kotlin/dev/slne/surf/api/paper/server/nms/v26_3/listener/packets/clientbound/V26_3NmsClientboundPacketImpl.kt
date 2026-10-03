package dev.slne.surf.api.paper.server.nms.v26_3.listener.packets.clientbound

import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.listener.packets.clientbound.NmsClientboundPacket
import dev.slne.surf.api.paper.server.nms.v26_3.listener.packets.V26_3NmsPacketImpl
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.common.ClientCommonPacketListener

@NmsUseWithCaution
@Suppress("ClassName")
abstract class V26_3NmsClientboundPacketImpl<Nms : Packet<Listener>, Listener : ClientCommonPacketListener>(
    nmsPacket: Nms,
) : NmsClientboundPacket, V26_3NmsPacketImpl<Nms, Listener>(nmsPacket)

package dev.slne.surf.api.paper.server.nms.v1_21_11.listener.packets.clientbound

import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.listener.packets.clientbound.NmsClientboundPacket
import dev.slne.surf.api.paper.server.nms.v1_21_11.listener.packets.V1_21_11NmsPacketImpl
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.common.ClientCommonPacketListener

@NmsUseWithCaution
abstract class V1_21_11NmsClientboundPacketImpl<Nms : Packet<Listener>, Listener : ClientCommonPacketListener>(
    nmsPacket: Nms,
) : NmsClientboundPacket, V1_21_11NmsPacketImpl<Nms, Listener>(nmsPacket)

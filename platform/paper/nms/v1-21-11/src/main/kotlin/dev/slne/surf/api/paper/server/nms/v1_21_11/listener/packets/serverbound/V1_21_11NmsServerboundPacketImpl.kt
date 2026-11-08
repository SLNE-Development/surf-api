package dev.slne.surf.api.paper.server.nms.v1_21_11.listener.packets.serverbound

import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.server.nms.v1_21_11.listener.packets.V1_21_11NmsPacketImpl
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.common.ServerCommonPacketListener

@NmsUseWithCaution
abstract class V1_21_11NmsServerboundPacketImpl<Nms : Packet<out ServerCommonPacketListener>>(nmsPacket: Nms) :
    V1_21_11NmsPacketImpl<Nms, ServerCommonPacketListener>(nmsPacket)

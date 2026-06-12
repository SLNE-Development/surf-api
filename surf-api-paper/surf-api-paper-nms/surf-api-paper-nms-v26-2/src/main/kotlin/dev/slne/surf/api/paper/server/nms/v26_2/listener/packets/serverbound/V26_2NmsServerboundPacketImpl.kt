package dev.slne.surf.api.paper.server.nms.v26_2.listener.packets.serverbound

import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.server.nms.v26_2.listener.packets.V26_2NmsPacketImpl
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.common.ServerCommonPacketListener

@NmsUseWithCaution
@Suppress("ClassName")
abstract class V26_2NmsServerboundPacketImpl<Nms : Packet<out ServerCommonPacketListener>>(nmsPacket: Nms) :
    V26_2NmsPacketImpl<Nms, ServerCommonPacketListener>(nmsPacket)

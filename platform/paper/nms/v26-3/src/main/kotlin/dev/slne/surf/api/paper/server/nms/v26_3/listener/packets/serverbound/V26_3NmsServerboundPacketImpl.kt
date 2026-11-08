package dev.slne.surf.api.paper.server.nms.v26_3.listener.packets.serverbound

import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.server.nms.v26_3.listener.packets.V26_3NmsPacketImpl
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.common.ServerCommonPacketListener

@NmsUseWithCaution
@Suppress("ClassName")
abstract class V26_3NmsServerboundPacketImpl<Nms : Packet<out ServerCommonPacketListener>>(nmsPacket: Nms) :
    V26_3NmsPacketImpl<Nms, ServerCommonPacketListener>(nmsPacket)

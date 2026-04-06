package dev.slne.surf.api.paper.server.impl.nms.listener.packets.serverbound

import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.server.impl.nms.listener.packets.NmsPacketImpl
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.common.ServerCommonPacketListener

@NmsUseWithCaution
abstract class NmsServerboundPacketImpl<Nms : Packet<out ServerCommonPacketListener>>(nmsPacket: Nms) :
    NmsPacketImpl<Nms, ServerCommonPacketListener>(nmsPacket)

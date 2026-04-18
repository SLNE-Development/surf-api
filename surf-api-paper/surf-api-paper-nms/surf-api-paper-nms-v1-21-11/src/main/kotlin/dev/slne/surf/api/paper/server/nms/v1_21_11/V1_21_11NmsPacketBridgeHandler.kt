package dev.slne.surf.api.paper.server.nms.v1_21_11

import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.common.NmsPacketBridgeHandler
import dev.slne.surf.api.paper.nms.listener.packets.NmsPacket
import dev.slne.surf.api.paper.nms.listener.packets.clientbound.NmsClientboundPacket
import dev.slne.surf.api.paper.nms.listener.packets.serverbound.NmsServerboundPacket
import dev.slne.surf.api.paper.server.nms.v1_21_11.listener.packets.V1_21_11NmsPacketImpl
import dev.slne.surf.api.paper.server.nms.v1_21_11.listener.packets.V1_21_11PacketRegistry
import net.minecraft.network.protocol.Packet

@NmsUseWithCaution
object V1_21_11NmsPacketBridgeHandler : NmsPacketBridgeHandler {

    @Suppress("UNCHECKED_CAST")
    override fun wrapServerboundPacket(nmsPacket: Any): NmsServerboundPacket? {
        return V1_21_11PacketRegistry.createServerboundPacketOrNull(nmsPacket as Packet<*>)
    }

    @Suppress("UNCHECKED_CAST")
    override fun wrapClientboundPacket(nmsPacket: Any): NmsClientboundPacket? {
        return V1_21_11PacketRegistry.createClientboundPacketOrNull(nmsPacket as Packet<*>)
    }

    override fun unwrapPacket(apiPacket: NmsPacket): Any {
        return V1_21_11NmsPacketImpl.getFromApi(apiPacket).nmsPacket
    }
}

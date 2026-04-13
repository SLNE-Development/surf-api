package dev.slne.surf.api.paper.server.nms.v26_1

import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.common.NmsPacketBridgeHandler
import dev.slne.surf.api.paper.nms.listener.packets.NmsPacket
import dev.slne.surf.api.paper.nms.listener.packets.clientbound.NmsClientboundPacket
import dev.slne.surf.api.paper.nms.listener.packets.serverbound.NmsServerboundPacket
import dev.slne.surf.api.paper.server.nms.v26_1.listener.packets.V26_1NmsPacketImpl
import dev.slne.surf.api.paper.server.nms.v26_1.listener.packets.V26_1PacketRegistry
import net.minecraft.network.protocol.Packet

@NmsUseWithCaution
class V26_1NmsPacketBridgeHandler : NmsPacketBridgeHandler {

    @Suppress("UNCHECKED_CAST")
    override fun wrapServerboundPacket(nmsPacket: Any): NmsServerboundPacket? {
        return V26_1PacketRegistry.createServerboundPacketOrNull(nmsPacket as Packet<*>)
    }

    @Suppress("UNCHECKED_CAST")
    override fun wrapClientboundPacket(nmsPacket: Any): NmsClientboundPacket? {
        return V26_1PacketRegistry.createClientboundPacketOrNull(nmsPacket as Packet<*>)
    }

    override fun unwrapPacket(apiPacket: NmsPacket): Any {
        return V26_1NmsPacketImpl.getFromApi(apiPacket).nmsPacket
    }
}

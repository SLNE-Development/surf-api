package dev.slne.surf.api.paper.server.nms.v26_2

import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.common.NmsPacketBridgeHandler
import dev.slne.surf.api.paper.nms.listener.packets.NmsPacket
import dev.slne.surf.api.paper.nms.listener.packets.clientbound.NmsClientboundPacket
import dev.slne.surf.api.paper.nms.listener.packets.serverbound.NmsServerboundPacket
import dev.slne.surf.api.paper.server.nms.v26_2.listener.packets.V26_2NmsPacketImpl
import dev.slne.surf.api.paper.server.nms.v26_2.listener.packets.V26_2PacketRegistry
import net.minecraft.network.protocol.Packet

@NmsUseWithCaution
@Suppress("ClassName")
object V26_2NmsPacketBridgeHandler : NmsPacketBridgeHandler {

    @Suppress("UNCHECKED_CAST")
    override fun wrapServerboundPacket(nmsPacket: Any): NmsServerboundPacket? {
        return V26_2PacketRegistry.createServerboundPacketOrNull(
            nmsPacket as Packet<*>
        )
    }

    @Suppress("UNCHECKED_CAST")
    override fun wrapClientboundPacket(nmsPacket: Any): NmsClientboundPacket? {
        return V26_2PacketRegistry.createClientboundPacketOrNull(
            nmsPacket as Packet<*>
        )
    }

    override fun unwrapPacket(apiPacket: NmsPacket): Any {
        return V26_2NmsPacketImpl.getFromApi(
            apiPacket
        ).nmsPacket
    }
}

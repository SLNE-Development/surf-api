package dev.slne.surf.api.paper.server.nms.v26_1.listener.packets

import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.listener.packets.NmsPacket
import net.minecraft.network.PacketListener
import net.minecraft.network.protocol.Packet

@NmsUseWithCaution
@Suppress("ClassName")
abstract class V26_1NmsPacketImpl<Nms : Packet<out Listener>, Listener : PacketListener>(var nmsPacket: Nms) :
    NmsPacket {
    val nmsClass = nmsPacket.javaClass

    override val packetClass =
        javaClass.interfaces.find { NmsPacket::class.java.isAssignableFrom(it) }
            ?: error("No packet interface found for ${javaClass.name}")

    companion object {
        @JvmStatic
        fun getFromApi(nmsPacket: NmsPacket): V26_1NmsPacketImpl<*, *> {
            require(nmsPacket is V26_1NmsPacketImpl<*, *>) { "Invalid NmsPacket implementation: " + nmsPacket.javaClass.getName() }
            return nmsPacket
        }
    }
}

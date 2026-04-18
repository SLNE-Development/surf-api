package dev.slne.surf.api.paper.server.nms.v1_21_11.listener.packets

import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.listener.packets.NmsPacket
import net.minecraft.network.PacketListener
import net.minecraft.network.protocol.Packet

@NmsUseWithCaution
abstract class V1_21_11NmsPacketImpl<Nms : Packet<out Listener>, Listener : PacketListener>(var nmsPacket: Nms) :
    NmsPacket {
    val nmsClass = nmsPacket.javaClass

    override val packetClass =
        javaClass.interfaces.find { NmsPacket::class.java.isAssignableFrom(it) }
            ?: error("No packet interface found for ${javaClass.name}")

    companion object {
        @JvmStatic
        fun getFromApi(nmsPacket: NmsPacket): V1_21_11NmsPacketImpl<*, *> {
            require(nmsPacket is V1_21_11NmsPacketImpl<*, *>) { "Invalid NmsPacket implementation: " + nmsPacket.javaClass.getName() }
            return nmsPacket
        }
    }
}

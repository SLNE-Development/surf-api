package dev.slne.surf.surfapi.bukkit.server.impl.nms.listener.packets

import dev.slne.surf.surfapi.bukkit.api.nms.NmsUseWithCaution
import dev.slne.surf.surfapi.bukkit.api.nms.listener.packets.NmsPacket
import net.minecraft.network.PacketListener
import net.minecraft.network.protocol.Packet

@NmsUseWithCaution
abstract class NmsPacketImpl<Nms : Packet<out Listener>, Listener : PacketListener>(var nmsPacket: Nms) :
    NmsPacket {
    val nmsClass = nmsPacket.javaClass as Class<Nms>
    override val packetClass =
        javaClass.interfaces.find { NmsPacket::class.java.isAssignableFrom(it) }
            ?: error("No packet interface found for ${javaClass.name}")

    companion object {
        @JvmStatic
        fun getFromApi(nmsPacket: NmsPacket): NmsPacketImpl<*, *> {
            require(nmsPacket is NmsPacketImpl<*, *>) { "Invalid NmsPacket implementation: " + nmsPacket.javaClass.getName() }
            return nmsPacket
        }
    }
}

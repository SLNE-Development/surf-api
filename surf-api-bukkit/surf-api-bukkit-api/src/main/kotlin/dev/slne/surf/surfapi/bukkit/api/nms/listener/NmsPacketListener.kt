package dev.slne.surf.surfapi.bukkit.api.nms.listener

import com.google.common.reflect.TypeToken
import dev.slne.surf.surfapi.bukkit.api.nms.NmsUseWithCaution
import dev.slne.surf.surfapi.bukkit.api.nms.listener.packets.NmsPacket

@NmsUseWithCaution
@Suppress("unused")
sealed interface NmsPacketListener<Packet : NmsPacket> {
    val packetMatcher: Class<in Packet>
        get() {
            val typeToken = object : TypeToken<Packet>(javaClass) {}
            return typeToken.rawType
        }
}

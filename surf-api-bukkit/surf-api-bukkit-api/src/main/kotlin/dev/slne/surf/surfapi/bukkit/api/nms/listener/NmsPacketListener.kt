package dev.slne.surf.surfapi.bukkit.api.nms.listener

import dev.slne.surf.surfapi.bukkit.api.nms.NmsUseWithCaution
import dev.slne.surf.surfapi.bukkit.api.nms.listener.packets.NmsPacket
import dev.slne.surf.surfapi.core.api.util.SurfTypeParameterMatcher

@NmsUseWithCaution
@Suppress("unused")
sealed interface NmsPacketListener<Packet : NmsPacket> {
    val packetMatcher: SurfTypeParameterMatcher
        get() = SurfTypeParameterMatcher.find(this, NmsPacketListener::class.java, "Packet")
}

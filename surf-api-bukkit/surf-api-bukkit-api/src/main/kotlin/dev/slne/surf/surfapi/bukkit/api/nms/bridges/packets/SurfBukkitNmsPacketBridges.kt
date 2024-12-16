package dev.slne.surf.surfapi.bukkit.api.nms.bridges.packets

import dev.slne.surf.surfapi.bukkit.api.nms.NmsUseWithCaution
import dev.slne.surf.surfapi.core.api.util.requiredService

@NmsUseWithCaution
interface SurfBukkitNmsPacketBridges {
    fun createEmptyPacketOperation(): PacketOperation

    companion object {
        val instance = requiredService<SurfBukkitNmsPacketBridges>()
    }
}

@NmsUseWithCaution
val nmsPacketBridges get() = SurfBukkitNmsPacketBridges.instance
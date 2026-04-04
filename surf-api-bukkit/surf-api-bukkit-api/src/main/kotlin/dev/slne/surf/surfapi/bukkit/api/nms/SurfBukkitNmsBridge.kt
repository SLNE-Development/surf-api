package dev.slne.surf.surfapi.bukkit.api.nms

import dev.slne.surf.surfapi.bukkit.api.nms.listener.NmsClientboundPacketListener
import dev.slne.surf.surfapi.bukkit.api.nms.listener.NmsServerboundPacketListener
import dev.slne.surf.surfapi.core.api.util.requiredService

@NmsUseWithCaution
interface SurfBukkitNmsBridge {

    fun registerServerboundPacketListener(listener: NmsServerboundPacketListener<*>)
    fun unregisterServerboundPacketListener(listener: NmsServerboundPacketListener<*>)

    fun registerClientboundPacketListener(listener: NmsClientboundPacketListener<*>)
    fun unregisterClientboundPacketListener(listener: NmsClientboundPacketListener<*>)

    companion object : SurfBukkitNmsBridge by bridge {
        val instance get() = bridge
    }
}

@NmsUseWithCaution
val bridge = requiredService<SurfBukkitNmsBridge>()

package dev.slne.surf.surfapi.bukkit.api.nms.bridges

import dev.slne.surf.surfapi.bukkit.api.nms.NmsUseWithCaution
import dev.slne.surf.surfapi.core.api.util.requiredService
import org.bukkit.entity.Entity

@NmsUseWithCaution
interface SurfBukkitNmsGlowingBridge {

    fun getCurrentFlags(entity: Entity): Byte

    companion object {
        val instance = requiredService<SurfBukkitNmsGlowingBridge>()
    }
}

@NmsUseWithCaution
val glowingBridge get() = SurfBukkitNmsGlowingBridge.instance
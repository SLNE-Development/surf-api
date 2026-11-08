package dev.slne.surf.api.paper.nms.bridges

import dev.slne.surf.api.core.util.requiredService
import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import org.bukkit.entity.Entity

@NmsUseWithCaution
interface SurfPaperNmsGlowingBridge {

    fun getCurrentFlags(entity: Entity): Byte

    companion object : SurfPaperNmsGlowingBridge by bridge {
        val INSTANCE get() = bridge
    }
}

@OptIn(NmsUseWithCaution::class)
private val bridge = requiredService<SurfPaperNmsGlowingBridge>()
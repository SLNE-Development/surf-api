package dev.slne.surf.api.paper.nms.bridges

import dev.slne.surf.api.core.util.requiredService
import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import org.bukkit.entity.Player

@NmsUseWithCaution
interface SurfPaperNmsStatsBridge {
    fun getPlayerStatsAsJson(player: Player): String
    fun savePlayerStatsToFile(player: Player)

    companion object : SurfPaperNmsStatsBridge by bridge {
        val INSTANCE get() = bridge
    }
}

@OptIn(NmsUseWithCaution::class)
private val bridge = requiredService<SurfPaperNmsStatsBridge>()

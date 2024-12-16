package dev.slne.surf.surfapi.bukkit.api.nms.bridges

import dev.slne.surf.surfapi.bukkit.api.nms.NmsUseWithCaution
import dev.slne.surf.surfapi.core.api.util.requiredService
import org.bukkit.entity.Player

@NmsUseWithCaution
interface SurfBukkitNmsStatsBridge {
    fun getPlayerStatsAsJson(player: Player): String
    fun savePlayerStatsToFile(player: Player)

    companion object {
        val instance = requiredService<SurfBukkitNmsStatsBridge>()
    }
}

@NmsUseWithCaution
val nmsStatsBridge get() = SurfBukkitNmsStatsBridge.instance

@file:OptIn(InternalSurfApi::class)

package dev.slne.surf.api.paper.nms.bridges

import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.shared.api.util.InternalSurfApi
import org.bukkit.entity.Player

@NmsUseWithCaution
interface SurfPaperNmsStatsBridge {
    fun getPlayerStatsAsJson(player: Player): String
    fun savePlayerStatsToFile(player: Player)
}

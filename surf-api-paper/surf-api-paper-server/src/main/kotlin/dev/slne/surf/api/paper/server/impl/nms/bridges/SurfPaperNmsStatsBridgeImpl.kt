package dev.slne.surf.api.paper.server.impl.nms.bridges

import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.bridges.SurfPaperNmsStatsBridge
import dev.slne.surf.api.paper.server.nms.toNms
import dev.slne.surf.api.paper.server.reflection.Reflection
import org.bukkit.entity.Player

@NmsUseWithCaution
class SurfPaperNmsStatsBridgeImpl : SurfPaperNmsStatsBridge {
    init {
    }

    override fun getPlayerStatsAsJson(player: Player): String {
        val gson = Reflection.SERVER_STATS_COUNTER_PROXY.getGson()
        val jsonElement = Reflection.SERVER_STATS_COUNTER_PROXY.toJson(player.toNms().stats)
        return gson.toJson(jsonElement)
    }

    override fun savePlayerStatsToFile(player: Player) {
        player.toNms().stats.save()
    }
}

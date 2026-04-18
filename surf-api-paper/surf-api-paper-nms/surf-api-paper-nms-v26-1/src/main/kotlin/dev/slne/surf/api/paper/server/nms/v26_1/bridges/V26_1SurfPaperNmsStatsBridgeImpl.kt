package dev.slne.surf.api.paper.server.nms.v26_1.bridges

import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.bridges.SurfPaperNmsStatsBridge
import dev.slne.surf.api.paper.server.nms.v26_1.extensions.toNms
import dev.slne.surf.api.paper.server.nms.v26_1.reflection.V26_1Reflection
import org.bukkit.entity.Player

@NmsUseWithCaution
@Suppress("ClassName")
class V26_1SurfPaperNmsStatsBridgeImpl : SurfPaperNmsStatsBridge {

    override fun getPlayerStatsAsJson(player: Player): String {
        val gson = V26_1Reflection.SERVER_STATS_COUNTER_PROXY.getGson()
        val jsonElement = V26_1Reflection.SERVER_STATS_COUNTER_PROXY.toJson(player.toNms().stats)
        return gson.toJson(jsonElement)
    }

    override fun savePlayerStatsToFile(player: Player) {
        player.toNms().stats.save()
    }
}

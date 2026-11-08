package dev.slne.surf.api.paper.server.nms.v26_2.bridges

import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.bridges.SurfPaperNmsStatsBridge
import dev.slne.surf.api.paper.server.nms.v26_2.extensions.toNms
import dev.slne.surf.api.paper.server.nms.v26_2.reflection.V26_2NmsReflections
import org.bukkit.entity.Player

@NmsUseWithCaution
@Suppress("ClassName")
class V26_2SurfPaperNmsStatsBridgeImpl : SurfPaperNmsStatsBridge {

    override fun getPlayerStatsAsJson(player: Player): String {
        val gson = V26_2NmsReflections.getServerStatsCounterGson()
        val jsonElement = V26_2NmsReflections.convertServerStatsCounterToJson(player.toNms().stats)
        return gson.toJson(jsonElement)
    }

    override fun savePlayerStatsToFile(player: Player) {
        player.toNms().stats.save()
    }
}

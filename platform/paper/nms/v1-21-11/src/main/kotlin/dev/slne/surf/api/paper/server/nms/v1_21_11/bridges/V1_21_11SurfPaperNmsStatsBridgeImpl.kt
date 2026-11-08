package dev.slne.surf.api.paper.server.nms.v1_21_11.bridges

import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.bridges.SurfPaperNmsStatsBridge
import dev.slne.surf.api.paper.server.nms.v1_21_11.extensions.toNms
import dev.slne.surf.api.paper.server.nms.v1_21_11.reflection.V1_21_11NmsReflections
import org.bukkit.entity.Player

@NmsUseWithCaution
class V1_21_11SurfPaperNmsStatsBridgeImpl : SurfPaperNmsStatsBridge {

    override fun getPlayerStatsAsJson(player: Player): String {
        val gson = V1_21_11NmsReflections.getServerStatsCounterGson()
        val jsonElement = V1_21_11NmsReflections.convertServerStatsCounterToJson(player.toNms().stats)
        return gson.toJson(jsonElement)
    }

    override fun savePlayerStatsToFile(player: Player) {
        player.toNms().stats.save()
    }
}

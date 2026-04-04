package dev.slne.surf.api.paper.server.impl.nms.bridges

import com.google.auto.service.AutoService
import dev.slne.surf.api.core.util.checkInstantiationByServiceLoader
import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.bridges.SurfPaperNmsStatsBridge
import dev.slne.surf.api.paper.server.nms.toNms
import dev.slne.surf.api.paper.server.reflection.Reflection
import org.bukkit.entity.Player

@AutoService(SurfPaperNmsStatsBridge::class)
@NmsUseWithCaution
class SurfPaperNmsStatsBridgeImpl : SurfPaperNmsStatsBridge {
    init {
        checkInstantiationByServiceLoader()
    }

    override fun getPlayerStatsAsJson(player: Player): String {
        return Reflection.SERVER_STATS_COUNTER_PROXY.toJson(player.toNms().stats)
    }

    override fun savePlayerStatsToFile(player: Player) {
        player.toNms().stats.save()
    }
}

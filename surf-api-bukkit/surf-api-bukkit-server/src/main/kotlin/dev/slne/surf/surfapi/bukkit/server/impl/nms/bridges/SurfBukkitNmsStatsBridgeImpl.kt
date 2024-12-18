package dev.slne.surf.surfapi.bukkit.server.impl.nms.bridges

import com.google.auto.service.AutoService
import dev.slne.surf.surfapi.bukkit.api.nms.NmsUseWithCaution
import dev.slne.surf.surfapi.bukkit.api.nms.bridges.SurfBukkitNmsStatsBridge
import dev.slne.surf.surfapi.bukkit.server.nms.toNms
import dev.slne.surf.surfapi.bukkit.server.reflection.Reflection
import dev.slne.surf.surfapi.core.api.util.checkInstantiationByServiceLoader
import org.bukkit.entity.Player

@AutoService(SurfBukkitNmsStatsBridge::class)
@NmsUseWithCaution
class SurfBukkitNmsStatsBridgeImpl : SurfBukkitNmsStatsBridge {
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

package dev.slne.surf.api.paper.server.nms.v1_21_11.glow.block

import dev.slne.surf.api.paper.extensions.server
import org.bukkit.Location
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class BlockPlayerData(val uuid: UUID) {
    val blocks = ConcurrentHashMap<Location, BlockGlowingData>()
    val player get() = server.getPlayer(uuid)
}

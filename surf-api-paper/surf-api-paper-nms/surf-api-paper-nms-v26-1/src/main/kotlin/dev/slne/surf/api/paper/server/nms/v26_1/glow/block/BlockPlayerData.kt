package dev.slne.surf.api.paper.server.nms.v26_1.glow.block

import dev.slne.surf.api.core.util.mutableObject2ObjectMapOf
import dev.slne.surf.api.paper.extensions.server
import org.bukkit.Location
import java.util.*

class BlockPlayerData(val uuid: UUID) {
    val blocks = mutableObject2ObjectMapOf<Location, V26_1BlockGlowingData>()
    val player get() = server.getPlayer(uuid)
}

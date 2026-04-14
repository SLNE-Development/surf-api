package dev.slne.surf.api.paper.server.nms.v1_21_11.glow.block

import dev.slne.surf.api.core.util.mutableObject2ObjectMapOf
import dev.slne.surf.api.paper.extensions.server
import org.bukkit.Location
import java.util.*

class BlockPlayerData(val uuid: UUID) {
    val blocks = mutableObject2ObjectMapOf<Location, V1_21_11BlockGlowingData>()
    val player get() = server.getPlayer(uuid)
}

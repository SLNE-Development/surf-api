package dev.slne.surf.surfapi.bukkit.server.impl.glow.block

import dev.slne.surf.surfapi.bukkit.api.extensions.server
import dev.slne.surf.surfapi.core.api.util.mutableObject2ObjectMapOf
import org.bukkit.Location
import java.util.*

class BlockPlayerData(val uuid: UUID) {
    val blocks = mutableObject2ObjectMapOf<Location, BlockGlowingData>()
    val player get() = server.getPlayer(uuid)
}
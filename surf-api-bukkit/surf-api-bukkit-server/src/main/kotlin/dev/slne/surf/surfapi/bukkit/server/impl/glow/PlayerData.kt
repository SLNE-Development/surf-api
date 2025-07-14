package dev.slne.surf.surfapi.bukkit.server.impl.glow

import dev.slne.surf.surfapi.core.api.util.mutableInt2ObjectMapOf
import java.util.*

data class PlayerData(val uuid: UUID) {
    val entities = mutableInt2ObjectMapOf<GlowingData>()
}
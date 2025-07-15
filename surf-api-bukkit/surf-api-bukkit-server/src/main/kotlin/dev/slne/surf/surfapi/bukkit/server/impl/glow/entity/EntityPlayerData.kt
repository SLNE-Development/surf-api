package dev.slne.surf.surfapi.bukkit.server.impl.glow.entity

import dev.slne.surf.surfapi.core.api.util.mutableInt2ObjectMapOf
import java.util.*

data class EntityPlayerData(val uuid: UUID) {
    val entities = mutableInt2ObjectMapOf<EntityGlowingData>()
}
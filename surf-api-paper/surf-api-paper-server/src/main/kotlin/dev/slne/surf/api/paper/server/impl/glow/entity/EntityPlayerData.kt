package dev.slne.surf.api.paper.server.impl.glow.entity

import java.util.*
import java.util.concurrent.ConcurrentHashMap

data class EntityPlayerData(val uuid: UUID) {
    val entities = ConcurrentHashMap<Int, EntityGlowingData>()
}
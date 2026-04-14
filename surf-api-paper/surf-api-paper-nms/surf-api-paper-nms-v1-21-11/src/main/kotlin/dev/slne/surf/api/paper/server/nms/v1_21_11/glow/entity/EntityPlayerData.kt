package dev.slne.surf.api.paper.server.nms.v1_21_11.glow.entity

import java.util.*
import java.util.concurrent.ConcurrentHashMap

data class EntityPlayerData(val uuid: UUID) {
    val entities = ConcurrentHashMap<Int, EntityGlowingData>()
}

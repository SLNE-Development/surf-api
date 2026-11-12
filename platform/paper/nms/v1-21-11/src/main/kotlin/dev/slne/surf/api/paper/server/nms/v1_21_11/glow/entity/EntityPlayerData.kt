package dev.slne.surf.api.paper.server.nms.v1_21_11.glow.entity

import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@NmsUseWithCaution
data class EntityPlayerData(val uuid: UUID) {
    val entities = ConcurrentHashMap<Int, EntityGlowingData>()
}

package dev.slne.surf.api.paper.server.nms.v26_2.glow.entity

import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@NmsUseWithCaution
@Suppress("ClassName")
data class EntityPlayerData(val uuid: UUID) {
    val entities = ConcurrentHashMap<Int, V26_2EntityGlowingData>()
}

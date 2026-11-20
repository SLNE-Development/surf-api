@file:OptIn(InternalSurfApi::class)

package dev.slne.surf.api.paper.region

import dev.slne.surf.api.shared.api.util.InternalSurfApi
import dev.slne.surf.api.spring.surfBean
import io.papermc.paper.math.Position
import org.bukkit.World
import org.bukkit.entity.Entity
import org.bukkit.util.BoundingBox
import dev.slne.surf.api.spring.SurfApiBean

@Suppress("UnstableApiUsage")
@SurfApiBean
interface TickThreadGuard {
    fun ensureTickThread(world: World, pos: Position, reason: String)
    fun ensureTickThread(world: World, pos: Position, blockRadius: Int, reason: String)
    fun ensureTickThread(world: World, chunkX: Int, chunkZ: Int, reason: String)

    fun ensureTickThread(entity: Entity, reason: String)

    fun ensureTickThread(world: World, box: BoundingBox, reason: String)
    fun ensureTickThread(world: World, blockX: Double, blockZ: Double, reason: String)

    fun ensureGlobalTickThread(reason: String)

    companion object {
        @InternalSurfApi
        val INSTANCE get() = tickThreadGuard
    }
}

private val tickThreadGuard by surfBean<TickThreadGuard>()

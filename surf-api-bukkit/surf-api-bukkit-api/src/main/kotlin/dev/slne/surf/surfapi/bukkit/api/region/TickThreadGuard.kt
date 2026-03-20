package dev.slne.surf.surfapi.bukkit.api.region

import dev.slne.surf.surfapi.core.api.util.requiredService
import io.papermc.paper.math.Position
import org.bukkit.World
import org.bukkit.entity.Entity
import org.bukkit.util.BoundingBox

@Suppress("UnstableApiUsage")
interface TickThreadGuard {

    fun ensureTickThread(world: World, pos: Position, reason: String)
    fun ensureTickThread(world: World, pos: Position, blockRadius: Int, reason: String)
    fun ensureTickThread(world: World, chunkX: Int, chunkZ: Int, reason: String)

    fun ensureTickThread(entity: Entity, reason: String)

    fun ensureTickThread(world: World, box: BoundingBox, reason: String)
    fun ensureTickThread(world: World, blockX: Double, blockZ: Double, reason: String)

    companion object : TickThreadGuard by tickThreadGuard {
        val instance = tickThreadGuard
    }
}

val tickThreadGuard = requiredService<TickThreadGuard>()
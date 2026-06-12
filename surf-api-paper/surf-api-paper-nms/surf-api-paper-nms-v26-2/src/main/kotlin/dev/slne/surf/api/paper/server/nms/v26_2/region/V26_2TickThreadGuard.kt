package dev.slne.surf.api.paper.server.nms.v26_2.region

import ca.spottedleaf.moonrise.common.util.TickThread
import dev.slne.surf.api.paper.region.TickThreadGuard
import dev.slne.surf.api.paper.server.nms.v26_2.extensions.toNms
import dev.slne.surf.api.paper.util.chunkX
import dev.slne.surf.api.paper.util.chunkZ
import io.papermc.paper.math.Position
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.AABB
import org.bukkit.World
import org.bukkit.entity.Entity
import org.bukkit.util.BoundingBox

@Suppress("UnstableApiUsage", "ClassName")
class V26_2TickThreadGuard : TickThreadGuard {

    override fun ensureTickThread(world: World, pos: Position, reason: String) {
        TickThread.ensureTickThread(world.toNms(), pos.chunkX, pos.chunkZ, reason)
    }

    override fun ensureTickThread(
        world: World,
        pos: Position,
        blockRadius: Int,
        reason: String
    ) {
        TickThread.ensureTickThread(
            world.toNms(),
            BlockPos(pos.blockX(), pos.blockY(), pos.blockZ()),
            blockRadius,
            reason
        )
    }

    override fun ensureTickThread(
        world: World,
        chunkX: Int,
        chunkZ: Int,
        reason: String
    ) {
        TickThread.ensureTickThread(world.toNms(), chunkX, chunkZ, reason)
    }

    override fun ensureTickThread(entity: Entity, reason: String) {
        TickThread.ensureTickThread(entity.toNms(), reason)
    }

    override fun ensureTickThread(world: World, box: BoundingBox, reason: String) {
        TickThread.ensureTickThread(
            world.toNms(),
            AABB(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ),
            reason
        )
    }

    override fun ensureTickThread(
        world: World,
        blockX: Double,
        blockZ: Double,
        reason: String
    ) {
        TickThread.ensureTickThread(world.toNms(), blockX, blockZ, reason)
    }
}

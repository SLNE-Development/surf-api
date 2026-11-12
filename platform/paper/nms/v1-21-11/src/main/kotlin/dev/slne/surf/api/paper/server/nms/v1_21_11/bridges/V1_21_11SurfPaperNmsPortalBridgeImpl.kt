package dev.slne.surf.api.paper.server.nms.v1_21_11.bridges

import ca.spottedleaf.moonrise.common.util.TickThread
import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.bridges.SurfPaperNmsPortalBridge
import dev.slne.surf.api.paper.server.nms.v1_21_11.extensions.toNms
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import org.bukkit.Location
import org.bukkit.World
import kotlin.jvm.optionals.getOrNull

@NmsUseWithCaution
@Suppress("ClassName")
class V1_21_11SurfPaperNmsPortalBridgeImpl : SurfPaperNmsPortalBridge {

    override fun findNearestNetherPortal(
        world: World,
        x: Int,
        y: Int,
        z: Int,
        searchRadius: Int,
        minX: Int,
        minZ: Int,
        maxXExclusive: Int,
        maxZExclusive: Int,
    ): Location? {
        val level = world.toNms()
        TickThread.ensureTickThread(level, x shr 4, z shr 4, "Cannot search for portals asynchronously")

        val found = level.portalForcer
            .findClosestPortalPosition(BlockPos(x, y, z), level.worldBorder, searchRadius)
            .getOrNull() ?: return null

        if (found.x !in minX..<maxXExclusive || found.z !in minZ..<maxZExclusive) {
            return null
        }

        return Location(world, found.x + 0.5, found.y.toDouble(), found.z + 0.5)
    }

    override fun createNetherPortal(
        world: World,
        x: Int,
        y: Int,
        z: Int,
        axisX: Boolean,
        creationRadius: Int,
    ): Location? {
        val level = world.toNms()
        TickThread.ensureTickThread(level, x shr 4, z shr 4, "Cannot create portals asynchronously")

        val axis = if (axisX) Direction.Axis.X else Direction.Axis.Z
        val rectangle = level.portalForcer
            .createPortal(BlockPos(x, y, z), axis, null, creationRadius)
            .getOrNull() ?: return null

        val corner = rectangle.minCorner
        val width = rectangle.axis1Size

        return Location(
            world,
            corner.x + if (axisX) width / 2.0 else 0.5,
            corner.y.toDouble(),
            corner.z + if (axisX) 0.5 else width / 2.0,
        )
    }
}

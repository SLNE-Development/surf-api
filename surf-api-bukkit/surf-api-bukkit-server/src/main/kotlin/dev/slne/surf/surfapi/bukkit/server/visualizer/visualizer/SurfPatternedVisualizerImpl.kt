package dev.slne.surf.surfapi.bukkit.server.visualizer.visualizer

import dev.slne.surf.surfapi.bukkit.api.nms.bridges.packets.entity.BlockDisplaySettings
import dev.slne.surf.surfapi.bukkit.api.visualizer.visualizer.SurfPatternedVisualizer
import dev.slne.surf.surfapi.bukkit.api.visualizer.visualizer.SurfVisualizer
import dev.slne.surf.surfapi.core.api.util.emptyObjectList
import dev.slne.surf.surfapi.core.api.util.mutableObjectListOf
import dev.slne.surf.surfapi.core.api.util.mutableObjectSetOf
import dev.slne.surf.surfapi.core.api.util.synchronize
import it.unimi.dsi.fastutil.objects.ObjectList
import org.bukkit.HeightMap
import org.bukkit.Location
import org.bukkit.Material
import kotlin.math.abs

class SurfPatternedVisualizerImpl : SurfVisualizerImpl(), SurfPatternedVisualizer {
    private val visualPoints = mutableObjectSetOf<Location>().synchronize()
    private var renderAtHighestPoint = true
    private var visualMaterial = SurfVisualizer.DEFAULT_MATERIAL
    private var visualMaterialSettings = BlockDisplaySettings()

    override fun addVisualPoint(point: Location) {
        require(visualPoints.firstOrNull()?.world == point.world) { "All points must be in the same world" }
        visualPoints.add(point.clone())

        if (running) {
            restartVisualizing()
        }
    }

    override fun removeVisualPoint(point: Location) {
        visualPoints.remove(point)

        if (running) {
            restartVisualizing()
        }
    }


    override fun setVisualMaterial(
        material: Material,
        consumer: BlockDisplaySettings.() -> Unit
    ) {
        visualMaterial = material
        visualMaterialSettings = BlockDisplaySettings.create(consumer)

        if (running) {
            restartVisualizing()
        }
    }

    override fun setVisualHeight(height: Int) {
        visualPoints.forEach { it.y = height.toDouble() }
        this.renderAtHighestPoint = false

        if (running) {
            restartVisualizing()
        }
    }

    override fun setRenderAtHighestPoint(renderAtHighestPoint: Boolean) {
        this.renderAtHighestPoint = renderAtHighestPoint
    }

    override fun startVisualizing(): Boolean {
        restartVisualizing()
        return super.startVisualizing()
    }

    private fun restartVisualizing() {
        removeAllVisualLocations()
        calculateLocationsFromPoints().forEach {
            addVisualLocation(it, visualMaterial, visualMaterialSettings)
        }
    }

    private fun calculateLocationsFromPoints(): ObjectList<Location> {
        // check if there are at least 2 points
        if (visualPoints.size < 2) {
            return emptyObjectList()
        }

        val locations = mutableObjectListOf<Location>()

        for (visualPoint in visualPoints) {
            if (renderAtHighestPoint) {
                locations.add(
                    visualPoint.getWorld().getHighestBlockAt(
                        visualPoint,
                        HeightMap.MOTION_BLOCKING_NO_LEAVES
                    ).location.clone().add(0.0, 1.0, 0.0)
                )
            } else {
                locations.add(visualPoint)
            }
        }

        locations.addAll(formLine(locations))

        return locations
    }

    private fun formLine(linePoints: List<Location>): ObjectList<Location> {
        val locations = mutableObjectListOf<Location>(linePoints)

        linePoints.forEachIndexed { index, location ->
            val nextIndex = if (index + 1 == linePoints.size) 0 else index + 1
            val nextLocation = linePoints[nextIndex]

            val points = walkPointAToB(location, nextLocation)
            locations.addAll(points.mapTo(mutableObjectListOf(points.size)) { it.clone() })
        }

        return locations
    }

    /**
     * Walks from point A to point B in a straight line, generating a list of locations along the
     * way.
     *
     * @param currentPoint The starting point of the walk.
     * @param nextPoint    The destination point of the walk.
     * @return A list of locations representing the path from point A to point B.
     */
    private fun walkPointAToB(
        currentPoint: Location,
        nextPoint: Location
    ): ObjectList<Location> { // TODO: is the y coordinate right here?
        val world = currentPoint.getWorld()
        val locations = mutableObjectListOf<Location>()

        var x1 = currentPoint.blockX()
        var y1 = currentPoint.blockY()
        var z1 = currentPoint.blockZ()
        val x2 = nextPoint.blockX()
        val y2 = nextPoint.blockY()
        val z2 = nextPoint.blockZ()

        val dx = abs(x2 - x1)
        val dy = abs(y2 - y1)
        val dz = abs(z2 - z1)

        val sx = if (x1 < x2) 1 else -1
        val sy = if (y1 < y2) 1 else -1
        val sz = if (z1 < z2) 1 else -1

        var err = dx - dy - dz

        while (true) {
            locations.add(Location(world, x1.toDouble(), y1.toDouble(), z1.toDouble()))

            if (x1 == x2 && y1 == y2 && z1 == z2) {
                break
            }

            val e2 = 2 * err

            if (e2 < dx) {
                err += dx
                y1 += sy
            }

            if (e2 > -dy) {
                err -= dy
                x1 += sx
            }

            if (e2 < dz) {
                err += dz
                z1 += sz
            }
        }

        return locations
    }
}

package dev.slne.surf.surfapi.bukkit.server.impl.visualizer.visualizer

import com.github.shynixn.mccoroutine.folia.launch
import dev.slne.surf.surfapi.bukkit.api.nms.bridges.packets.entity.BlockDisplaySettings
import dev.slne.surf.surfapi.bukkit.api.util.computeHighestYBlock
import dev.slne.surf.surfapi.bukkit.api.visualizer.visualizer.SurfVisualizer
import dev.slne.surf.surfapi.bukkit.api.visualizer.visualizer.SurfVisualizerArea
import dev.slne.surf.surfapi.bukkit.server.impl.visualizer.visualizerApiImpl
import dev.slne.surf.surfapi.bukkit.server.plugin
import dev.slne.surf.surfapi.core.api.algorithms.convexHull2D
import dev.slne.surf.surfapi.core.api.math.VoxelLineTracer
import dev.slne.surf.surfapi.core.api.util.toObjectSet
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet
import it.unimi.dsi.fastutil.objects.ObjectSet
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.bukkit.World
import org.spongepowered.math.vector.Vector3d
import kotlin.time.Duration

class SurfVisualizerAreaImpl(
    world: World,
    private val useHighestYBlock: Boolean,
    initialSettings: BlockDisplaySettings?,
    initialEdges: Collection<Vector3d>,
    private val placeDelay: Duration = Duration.ZERO,
    private val delegate: SurfVisualizerMultipleLocationsImpl = visualizerApiImpl.createMultiLocationVisualizer(
        world
    ),
) : SurfVisualizer by delegate, SurfVisualizerArea {

    private val corners = ObjectLinkedOpenHashSet(initialEdges)
    override val cornerLocations by lazy { corners.toObjectSet() }
    private val recomputationMutex = Mutex()

    override var settings: BlockDisplaySettings = initialSettings ?: BlockDisplaySettings.create {
        blockData = SurfVisualizer.DEFAULT_BLOCK_TYPE.createBlockData()
    }
        set(value) {
            field = value
            launchRecompute()
        }

    init {
        if (initialEdges.isNotEmpty()) {
            launchRecompute()
        }
    }

    override fun settings(consumer: BlockDisplaySettings.() -> Unit) {
        settings.consumer()
        launchRecompute()
    }

    override fun addCornerLocation(location: Vector3d) {
        if (corners.add(location)) {
            launchRecompute()
        }
    }

    override fun removeCornerLocation(location: Vector3d) {
        if (corners.remove(location)) {
            launchRecompute()
        }
    }

    override fun clearCornerLocations() {
        if (corners.isNotEmpty()) {
            corners.clear()
            launchRecompute()
        }
    }

    override fun setCornerLocations(locations: Collection<Vector3d>) {
        if (corners != locations) {
            corners.clear()
            corners.addAll(locations)
            launchRecompute()
        }
    }

    private fun launchRecompute() {
        plugin.launch {
            recompute()
        }
    }

    private suspend fun recompute() = recomputationMutex.withLock {
        delegate.clearVisualLocations()
        if (corners.size < 2) return
        if (!delegate.checkNotNullWorld()) return

        val hull = corners.convexHull2D()
        val cornerBlocks = hull

        val edgePoints = ObjectLinkedOpenHashSet<Vector3d>()
        for (i in cornerBlocks.indices) {
            edgePoints += VoxelLineTracer.trace(
                cornerBlocks[i],
                cornerBlocks[(i + 1) % cornerBlocks.size]
            )
        }
        val finalEdgePoints: ObjectSet<Vector3d> = if (useHighestYBlock) {
            edgePoints.map { it.toInt() }
                .computeHighestYBlock(delegate.world)
                .map { it.add(0, 1, 0).toDouble() }
                .toCollection(ObjectLinkedOpenHashSet())
        } else {
            edgePoints
        }

        if (placeDelay.isPositive()) {
            for ((i, point) in finalEdgePoints.withIndex()) {
                delegate.addVisualLocation(point, settings)
                if (i < finalEdgePoints.size - 1) {
                    delay(placeDelay)
                }
            }
        } else {
            delegate.addVisualLocations(finalEdgePoints, settings)
            finalEdgePoints.forEach {
                delegate.addVisualLocation(it, settings)
            }
        }

    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SurfVisualizerAreaImpl) return false

        if (delegate != other.delegate) return false

        return true
    }

    override fun hashCode(): Int {
        return delegate.hashCode()
    }

    override fun toString(): String {
        return "SurfVisualizerAreaImpl(useHighestYBlock=$useHighestYBlock, corners=$corners, settings=$settings)"
    }

}
package dev.slne.surf.surfapi.bukkit.server.impl.visualizer.visualizer

import com.github.shynixn.mccoroutine.folia.scope
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
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import org.bukkit.World
import org.spongepowered.math.vector.Vector3d
import java.util.concurrent.ConcurrentHashMap
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

    private val corners = ConcurrentHashMap.newKeySet<Vector3d>(initialEdges.size)
    override val cornerLocations get() = corners.toObjectSet()
    private val computationChannel = Channel<Unit>(Channel.CONFLATED)

    private val scope = CoroutineScope(
        plugin.scope.coroutineContext + SupervisorJob(plugin.scope.coroutineContext[Job])
    )

    override var settings: BlockDisplaySettings = initialSettings ?: BlockDisplaySettings.create {
        blockData = SurfVisualizer.DEFAULT_BLOCK_TYPE.createBlockData()
    }
        set(value) {
            field = value
            launchRecompute()
        }

    init {
        corners.addAll(initialEdges)
        if (corners.isNotEmpty()) {
            launchRecompute()
        }

        scope.launch {
            computationChannel.consumeEach {
                recompute()
            }
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
        computationChannel.trySend(Unit)
//        plugin.launch {
//            recompute()
//        }
    }

    private suspend fun recompute() {
        if (delegate.isClosed()) return
        val cornersSnapshot = ObjectLinkedOpenHashSet(corners)
        val settingsSnapshot = settings.clone()

        delegate.clearVisualLocations()
        if (cornersSnapshot.size < 2) return
        if (!delegate.checkNotNullWorld()) return

        currentCoroutineContext().ensureActive()

        val hull = cornersSnapshot.convexHull2D()
        val edgePoints = ObjectLinkedOpenHashSet<Vector3d>()

        for (i in hull.indices) {
            edgePoints += VoxelLineTracer.trace(
                hull[i],
                hull[(i + 1) % hull.size]
            )
        }

        currentCoroutineContext().ensureActive()

        val finalEdgePoints: ObjectSet<Vector3d> = if (useHighestYBlock) {
            edgePoints.map { it.toInt() }
                .computeHighestYBlock(delegate.world)
                .map { it.add(0, 1, 0).toDouble() }
                .toCollection(ObjectLinkedOpenHashSet())
        } else {
            edgePoints
        }

        currentCoroutineContext().ensureActive()

        if (placeDelay.isPositive()) {
            for ((i, point) in finalEdgePoints.withIndex()) {
                currentCoroutineContext().ensureActive()
                delegate.addVisualLocation(point, settingsSnapshot)
                if (i < finalEdgePoints.size - 1) {
                    delay(placeDelay)
                }
            }
        } else {
            delegate.addVisualLocations(finalEdgePoints, settingsSnapshot)
        }
    }

    override fun close() {
        scope.cancel("Visualizer closed.")
        computationChannel.close()
        delegate.close()
    }

    override fun stopVisualizing(): Boolean {
        scope.coroutineContext[Job]?.children?.forEach { it.cancel() }
        return delegate.stopVisualizing()
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
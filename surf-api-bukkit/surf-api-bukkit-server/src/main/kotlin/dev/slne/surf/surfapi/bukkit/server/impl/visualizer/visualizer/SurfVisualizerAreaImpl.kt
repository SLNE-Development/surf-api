package dev.slne.surf.surfapi.bukkit.server.impl.visualizer.visualizer

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.shynixn.mccoroutine.folia.scope
import dev.slne.surf.surfapi.bukkit.api.nms.bridges.packets.entity.BlockDisplaySettings
import dev.slne.surf.surfapi.bukkit.api.util.getHighestBlockYAtBlockCoordinates
import dev.slne.surf.surfapi.bukkit.api.util.getXFromChunkKey
import dev.slne.surf.surfapi.bukkit.api.util.getZFromChunkKey
import dev.slne.surf.surfapi.bukkit.api.util.isChunkVisible
import dev.slne.surf.surfapi.bukkit.api.visualizer.visualizer.SurfVisualizer
import dev.slne.surf.surfapi.bukkit.api.visualizer.visualizer.SurfVisualizerArea
import dev.slne.surf.surfapi.bukkit.server.impl.visualizer.visualizerApiImpl
import dev.slne.surf.surfapi.bukkit.server.plugin
import dev.slne.surf.surfapi.core.api.algorithms.convexHull2D
import dev.slne.surf.surfapi.core.api.math.VoxelLineTracer
import dev.slne.surf.surfapi.core.api.util.mutableLongSetOf
import dev.slne.surf.surfapi.core.api.util.toObjectSet
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.future.await
import org.bukkit.Bukkit
import org.bukkit.Chunk
import org.bukkit.ChunkSnapshot
import org.bukkit.World
import org.bukkit.entity.Player
import org.bukkit.util.NumberConversions
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

    private var workerJob: Job? = null

    override var settings: BlockDisplaySettings = initialSettings ?: BlockDisplaySettings.create {
        blockData = SurfVisualizer.DEFAULT_BLOCK_TYPE.createBlockData()
    }
        set(value) {
            field = value
            launchRecompute()
        }

    /**
     * Pending edge points grouped by chunk key, awaiting Y resolution.
     * These are the raw 2D points (y=0) from the hull computation.
     * Once a chunk becomes visible to any viewer, the points are resolved,
     * added to the delegate, and removed from this map.
     */
    private val pendingByChunk = ConcurrentHashMap<Long, MutableSet<Vector3d>>()

    /**
     * Cache of chunk snapshots for Y resolution. Avoids re-loading chunks
     * that were already resolved.
     */
    private val snapshotCache = Caffeine.newBuilder()
        .maximumSize(512)
        .build<Long, ChunkSnapshot>()

    /**
     * The settings snapshot captured at recompute time, used for lazily resolved points.
     */
    @Volatile
    private var pendingSettings: BlockDisplaySettings? = null

    init {
        corners.addAll(initialEdges)
        if (corners.isNotEmpty()) {
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
        computationChannel.trySend(Unit)
    }

    private fun startRecompute() {
        workerJob?.cancel("Visualizer recompute cancelled.")
        workerJob = scope.launch {
            computationChannel.consumeEach {
                recompute()
            }
        }
    }

    private suspend fun recompute() {
        if (delegate.isClosed()) return
        val cornersSnapshot = ObjectLinkedOpenHashSet(corners)
        val settingsSnapshot = settings.clone()

        // Clear all previous state
        delegate.clearVisualLocations()
        pendingByChunk.clear()
        snapshotCache.invalidateAll()
        pendingSettings = null

        if (cornersSnapshot.size < 2) return
        if (!delegate.checkNotNullWorld()) return

        currentCoroutineContext().ensureActive()

        // Compute 2D hull and edge points (cheap, no chunk loading)
        val hull = cornersSnapshot.convexHull2D()
        val edgePoints = ObjectLinkedOpenHashSet<Vector3d>()

        for (i in hull.indices) {
            edgePoints += VoxelLineTracer.trace(
                hull[i],
                hull[(i + 1) % hull.size]
            )
        }

        currentCoroutineContext().ensureActive()


        if (!useHighestYBlock) {
            // No height resolution needed — add all points directly
            if (placeDelay.isPositive()) {
                for ((i, point) in edgePoints.withIndex()) {
                    currentCoroutineContext().ensureActive()
                    delegate.addVisualLocation(point, settingsSnapshot)
                    if (i < edgePoints.size - 1) {
                        delay(placeDelay)
                    }
                }
            } else {
                delegate.addVisualLocations(edgePoints, settingsSnapshot)
            }
            return
        }
        pendingSettings = settingsSnapshot

        // Group edge points by chunk
        for (point in edgePoints) {
            val chunkKey = Chunk.getChunkKey(
                NumberConversions.floor(point.x()) shr 4,
                NumberConversions.floor(point.z()) shr 4
            )
            pendingByChunk.computeIfAbsent(chunkKey) {
                ConcurrentHashMap.newKeySet()
            }.add(point)
        }

        currentCoroutineContext().ensureActive()

        // Immediately resolve chunks that any viewer can already see
        if (delegate.isVisualizing() && delegate.hasViewers()) {
            resolveVisibleChunks(settingsSnapshot)
        }
    }

    /**
     * Resolves pending chunks that are currently visible to at least one viewer.
     * Called after recompute and after startVisualizing.
     */
    private suspend fun resolveVisibleChunks(settingsSnapshot: BlockDisplaySettings) {
        val world = delegate.world

        // Collect chunk keys that any viewer can see
        val visibleKeys = mutableLongSetOf()
        for (viewerUuid in delegate.viewerUuids) {
            val player = Bukkit.getPlayer(viewerUuid) ?: continue
            for (chunkKey in pendingByChunk.keys) {
                val cx = getXFromChunkKey(chunkKey)
                val cz = getZFromChunkKey(chunkKey)
                if (player.isChunkVisible(world, cx, cz)) {
                    visibleKeys.add(chunkKey)
                }
            }
        }

        val iterator = visibleKeys.iterator()
        while (iterator.hasNext()) {
            val chunkKey = iterator.nextLong()
            currentCoroutineContext().ensureActive()
            resolveChunk(chunkKey, settingsSnapshot)
        }
    }

    private suspend fun resolveChunk(chunkKey: Long, settingsSnapshot: BlockDisplaySettings) {
        val points = pendingByChunk.remove(chunkKey) ?: return
        val world = delegate.getWorldIfPresent() ?: return

        val snapshot = getOrLoadSnapshot(chunkKey, world) ?: return

        val resolvedPoints = points.map { point ->
            val y = snapshot.getHighestBlockYAtBlockCoordinates(
                NumberConversions.floor(point.x()),
                NumberConversions.floor(point.z())
            )
            Vector3d(point.x(), (y + 1).toDouble(), point.z()) to settingsSnapshot
        }

        if (placeDelay.isPositive()) {
            for ((i, pair) in resolvedPoints.withIndex()) {
                currentCoroutineContext().ensureActive()
                delegate.addVisualLocation(pair.first, pair.second)
                if (i < resolvedPoints.size - 1) {
                    delay(placeDelay)
                }
            }
        } else {
            delegate.addVisualLocations(resolvedPoints)
        }
    }

    private suspend fun getOrLoadSnapshot(chunkKey: Long, world: World): ChunkSnapshot? {
        val cached = snapshotCache.getIfPresent(chunkKey)
        if (cached != null) return cached

        val cx = getXFromChunkKey(chunkKey)
        val cz = getZFromChunkKey(chunkKey)

        val chunk = world.getChunkAtAsync(cx, cz).await()
        val snapshot = chunk.getChunkSnapshot(true, false, false, false)
        snapshotCache.put(chunkKey, snapshot)
        return snapshot
    }

    fun onChunkBecameVisible(player: Player, chunk: Chunk) {
        if (!useHighestYBlock) return
        if (!delegate.isVisualizing()) return
        if (delegate.isClosed()) return

        val chunkKey = chunk.chunkKey
        val points = pendingByChunk.remove(chunkKey) ?: return

        // We're on the tick thread and the chunk is loaded — snapshot is cheap
        val snapshot = chunk.getChunkSnapshot(true, false, false, false)
        snapshotCache.put(chunkKey, snapshot)

        val settingsSnapshot = pendingSettings?.clone() ?: return

        val resolvedPoints = points.map { point ->
            val y = snapshot.getHighestBlockYAtBlockCoordinates(
                NumberConversions.floor(point.x()),
                NumberConversions.floor(point.z())
            )
            Vector3d(point.x(), (y + 1).toDouble(), point.z()) to settingsSnapshot
        }

        // Add to delegate — this will also spawn for the player since
        // delegate.isVisualizing() is true and the player is a viewer
        delegate.addVisualLocations(resolvedPoints)
    }

    override fun startVisualizing(): Boolean {
        startRecompute()
        val result = delegate.startVisualizing()
        if (result && useHighestYBlock && pendingByChunk.isNotEmpty()) {
            val settingsSnapshot = pendingSettings ?: return true
            scope.launch {
                resolveVisibleChunks(settingsSnapshot)
            }
        }
        return result
    }

    override fun stopVisualizing(): Boolean {
        scope.coroutineContext[Job]?.cancelChildren()
        return delegate.stopVisualizing()
    }

    override fun close() {
        scope.cancel("Visualizer closed.")
        computationChannel.close()
        pendingByChunk.clear()
        snapshotCache.invalidateAll()
        pendingSettings = null
        delegate.close()
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
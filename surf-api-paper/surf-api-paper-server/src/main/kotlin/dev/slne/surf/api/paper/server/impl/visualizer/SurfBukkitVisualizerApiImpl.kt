@file:OptIn(ExperimentalVisualizerApi::class)

package dev.slne.surf.api.paper.server.impl.visualizer

import com.github.benmanes.caffeine.cache.Caffeine
import com.google.auto.service.AutoService
import dev.slne.surf.api.paper.nms.bridges.packets.entity.BlockDisplaySettings
import dev.slne.surf.api.paper.server.impl.visualizer.visualizer.AbstractSurfVisualizerImpl
import dev.slne.surf.api.paper.server.impl.visualizer.visualizer.SurfVisualizerAreaImpl
import dev.slne.surf.api.paper.server.impl.visualizer.visualizer.SurfVisualizerMultipleLocationsImpl
import dev.slne.surf.api.paper.server.impl.visualizer.visualizer.SurfVisualizerSingleLocationImpl
import dev.slne.surf.api.paper.visualizer.SurfPaperVisualizerApi
import dev.slne.surf.api.paper.visualizer.visualizer.ExperimentalVisualizerApi
import dev.slne.surf.api.paper.visualizer.visualizer.SurfVisualizer
import dev.slne.surf.api.paper.visualizer.visualizer.SurfVisualizerArea
import dev.slne.surf.api.paper.visualizer.visualizer.SurfVisualizerSingleLocation
import org.bukkit.Chunk
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.Player
import org.spongepowered.math.vector.Vector3d
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration

@AutoService(SurfPaperVisualizerApi::class)
class SurfBukkitVisualizerApiImpl : SurfPaperVisualizerApi {
    private val visualizers = Caffeine.newBuilder()
        .weakValues()
        .build<UUID, AbstractSurfVisualizerImpl>()
    private val areaVisualizers = Caffeine.newBuilder()
        .weakValues()
        .build<UUID, SurfVisualizerAreaImpl>()

    private val playerToVisualizers = ConcurrentHashMap<UUID, MutableSet<UUID>>()

    override fun createSingleLocationVisualizer(location: Location): SurfVisualizerSingleLocation {
        return SurfVisualizerSingleLocationImpl(location).also { visualizers.put(it.uid, it) }
    }

    override fun createMultiLocationVisualizer(world: World): SurfVisualizerMultipleLocationsImpl {
        return SurfVisualizerMultipleLocationsImpl(world).also { visualizers.put(it.uid, it) }
    }

    override fun createAreaVisualizer(
        world: World,
        initialSettings: BlockDisplaySettings?,
        initialEdges: Collection<Vector3d>,
        useHighestYBlock: Boolean,
        placeDelay: Duration,
    ): SurfVisualizerArea {
        return SurfVisualizerAreaImpl(
            world,
            useHighestYBlock,
            initialSettings,
            initialEdges,
            placeDelay,
        ).also { areaVisualizers.put(it.uid, it) }
    }

    override fun getByUid(uid: UUID): SurfVisualizer? {
        return areaVisualizers.getIfPresent(uid) ?: visualizers.getIfPresent(uid)
    }

    fun onViewerAdded(visualizerUid: UUID, playerUid: UUID) {
        playerToVisualizers.computeIfAbsent(playerUid) { ConcurrentHashMap.newKeySet() }
            .add(visualizerUid)
    }

    fun onViewerRemoved(visualizerUid: UUID, playerUid: UUID) {
        playerToVisualizers.computeIfPresent(playerUid) { _, visualizerUids ->
            visualizerUids.remove(visualizerUid)
            visualizerUids.takeUnless { it.isEmpty() }
        }
    }

    fun processChunkReceiveUpdateForPlayer(player: Player, chunk: Chunk) {
        val visualizerUuids = playerToVisualizers[player.uniqueId] ?: return

        // Area visualizers must resolve their pending height points before their delegate
        // processes the same chunk.
        for (uid in visualizerUuids) {
            val area = areaVisualizers.getIfPresent(uid) ?: continue
            if (!area.isClosed() && area.isVisualizing()) {
                area.onChunkBecameVisible(chunk)
            }
        }

        for (uid in visualizerUuids) {
            val visualizer = visualizers.getIfPresent(uid) ?: continue
            if (!visualizer.isClosed() && visualizer.isVisualizing()) {
                visualizer.onPlayerReceiveChunk(player, chunk)
            }
        }
    }

    fun processChunkUnloadForPlayer(player: Player, chunk: Chunk) {
        val visualizerUuids = playerToVisualizers[player.uniqueId] ?: return
        for (uid in visualizerUuids) {
            val visualizer = visualizers.getIfPresent(uid) ?: continue
            if (!visualizer.isClosed() && visualizer.isVisualizing()) {
                visualizer.onPlayerUnloadChunk(player, chunk)
            }
        }
    }

    fun processPlayerQuit(player: Player) {
        val visualizerUuids = playerToVisualizers.remove(player.uniqueId) ?: return

        for (uid in visualizerUuids) {
            val visualizer = visualizers.getIfPresent(uid) ?: continue
            if (!visualizer.isClosed()) {
                visualizer.removeViewer(player)
            }
        }
    }

    fun onVisualizerClose(visualizer: AbstractSurfVisualizerImpl) {
        visualizers.invalidate(visualizer.uid)
        areaVisualizers.invalidate(visualizer.uid)
    }

    companion object {
        val INSTANCE get() = SurfPaperVisualizerApi.INSTANCE as SurfBukkitVisualizerApiImpl
    }
}

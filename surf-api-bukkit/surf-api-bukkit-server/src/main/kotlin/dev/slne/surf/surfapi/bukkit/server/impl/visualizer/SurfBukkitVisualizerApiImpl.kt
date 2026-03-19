package dev.slne.surf.surfapi.bukkit.server.impl.visualizer

import com.github.benmanes.caffeine.cache.Caffeine
import com.google.auto.service.AutoService
import dev.slne.surf.surfapi.bukkit.api.nms.bridges.packets.entity.BlockDisplaySettings
import dev.slne.surf.surfapi.bukkit.api.visualizer.SurfBukkitVisualizerApi
import dev.slne.surf.surfapi.bukkit.api.visualizer.visualizer.SurfVisualizer
import dev.slne.surf.surfapi.bukkit.api.visualizer.visualizer.SurfVisualizerArea
import dev.slne.surf.surfapi.bukkit.api.visualizer.visualizer.SurfVisualizerSingleLocation
import dev.slne.surf.surfapi.bukkit.server.impl.visualizer.visualizer.AbstractSurfVisualizerImpl
import dev.slne.surf.surfapi.bukkit.server.impl.visualizer.visualizer.SurfVisualizerAreaImpl
import dev.slne.surf.surfapi.bukkit.server.impl.visualizer.visualizer.SurfVisualizerMultipleLocationsImpl
import dev.slne.surf.surfapi.bukkit.server.impl.visualizer.visualizer.SurfVisualizerSingleLocationImpl
import org.bukkit.Chunk
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.Player
import org.spongepowered.math.vector.Vector3d
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration

@AutoService(SurfBukkitVisualizerApi::class)
class SurfBukkitVisualizerApiImpl : SurfBukkitVisualizerApi {
    private val visualizers = Caffeine.newBuilder()
        .weakValues()
        .build<UUID, AbstractSurfVisualizerImpl>()
    private val areaVisualizers = Caffeine.newBuilder()
        .weakValues()
        .build<UUID, SurfVisualizerArea>()

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

    private fun getActiveVisualizers(player: Player): List<AbstractSurfVisualizerImpl> {
        val visualizerUuids = playerToVisualizers[player.uniqueId] ?: return emptyList()
        return visualizerUuids.mapNotNull { uid ->
            visualizers.getIfPresent(uid)?.takeIf { !it.isClosed() && it.isVisualizing() }
        }
    }

    fun onViewerAdded(visualizerUid: UUID, playerUid: UUID) {
        playerToVisualizers.computeIfAbsent(playerUid) { ConcurrentHashMap.newKeySet() }
            .add(visualizerUid)
    }

    fun onViewerRemoved(visualizerUid: UUID, playerUid: UUID) {
        playerToVisualizers[playerUid]?.remove(visualizerUid)
    }

    fun processChunkReceiveUpdateForPlayer(player: Player, chunk: Chunk) {
        val active = getActiveVisualizers(player)
        active.forEach { it.onPlayerReceiveChunk(player, chunk) }
    }

    fun processChunkUnloadForPlayer(player: Player, chunk: Chunk) {
        val active = getActiveVisualizers(player)
        active.forEach { it.onPlayerUnloadChunk(player, chunk) }
    }

    fun processPlayerQuit(player: Player) {
        playerToVisualizers.remove(player.uniqueId)

        for (active in visualizers.asMap().values) {
            if (active.isClosed()) continue
            active.removeViewer(player)
        }
    }

    fun onVisualizerClose(visualizer: AbstractSurfVisualizerImpl) {
        visualizers.invalidate(visualizer.uid)
        areaVisualizers.invalidate(visualizer.uid)
    }
}

val visualizerApiImpl get() = SurfBukkitVisualizerApi.instance as SurfBukkitVisualizerApiImpl

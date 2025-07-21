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
import dev.slne.surf.surfapi.core.api.util.logger
import org.bukkit.Chunk
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.Player
import org.spongepowered.math.vector.Vector3d
import java.util.*
import kotlin.time.Duration

@AutoService(SurfBukkitVisualizerApi::class)
class SurfBukkitVisualizerApiImpl : SurfBukkitVisualizerApi {
    private val visualizers = Caffeine.newBuilder()
        .softValues()
        .build<UUID, AbstractSurfVisualizerImpl>()
    private val areaVisualizers = Caffeine.newBuilder()
        .softValues()
        .build<UUID, SurfVisualizerArea>()

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

    private fun getActiveVisualizers(player: Player) =
        visualizers.asMap().values.filter { it.isVisualizing() && it.visibleTo(player) }


    private val log = logger()
    fun processChunkReceiveUpdateForPlayer(player: Player, chunk: Chunk) {
        val active = getActiveVisualizers(player)

        if (active.isNotEmpty()) {
            log.atInfo()
                .log("Received update for player ${player.name} for ${active.size} visualizers")
        }

        active.forEach { it.onPlayerReceiveChunk(player, chunk) }
    }

    fun processChunkUnloadForPlayer(player: Player, chunk: Chunk) {
        val active = getActiveVisualizers(player)

        if (active.isNotEmpty()) {
            log.atInfo()
                .log("Received unload for player ${player.name} for ${active.size} visualizers")
        }

        active.forEach { it.onPlayerUnloadChunk(player, chunk) }
    }

    fun processPlayerQuit(player: Player) {
        val active = visualizers.asMap().values

        if (active.isNotEmpty()) {
            log.atInfo()
                .log("Player ${player.name} quit, removing from ${active.size} visualizers")
        }
        
        active.forEach { it.removeViewer(player) }
    }
}

val visualizerApiImpl get() = SurfBukkitVisualizerApi.instance as SurfBukkitVisualizerApiImpl

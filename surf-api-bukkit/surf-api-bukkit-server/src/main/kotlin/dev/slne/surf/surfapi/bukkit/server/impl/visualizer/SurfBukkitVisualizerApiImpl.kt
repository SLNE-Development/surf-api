package dev.slne.surf.surfapi.bukkit.server.impl.visualizer

import com.github.benmanes.caffeine.cache.Caffeine
import com.google.auto.service.AutoService
import dev.slne.surf.surfapi.bukkit.api.nms.bridges.packets.entity.BlockDisplaySettings
import dev.slne.surf.surfapi.bukkit.api.visualizer.SurfBukkitVisualizerApi
import dev.slne.surf.surfapi.bukkit.api.visualizer.visualizer.SurfVisualizer
import dev.slne.surf.surfapi.bukkit.api.visualizer.visualizer.SurfVisualizerArea
import dev.slne.surf.surfapi.bukkit.api.visualizer.visualizer.SurfVisualizerSingleLocation
import dev.slne.surf.surfapi.bukkit.server.impl.visualizer.visualizer.SurfVisualizerAreaImpl
import dev.slne.surf.surfapi.bukkit.server.impl.visualizer.visualizer.SurfVisualizerMultipleLocationsImpl
import dev.slne.surf.surfapi.bukkit.server.impl.visualizer.visualizer.SurfVisualizerSingleLocationImpl
import org.bukkit.Location
import org.bukkit.World
import org.spongepowered.math.vector.Vector3d
import java.util.*

@AutoService(SurfBukkitVisualizerApi::class)
class SurfBukkitVisualizerApiImpl : SurfBukkitVisualizerApi {
    private val visualizers = Caffeine.newBuilder()
        .softValues()
        .build<UUID, SurfVisualizer>()
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
    ): SurfVisualizerArea {
        return SurfVisualizerAreaImpl(
            world,
            useHighestYBlock,
            initialSettings,
            initialEdges
        ).also { areaVisualizers.put(it.uid, it) }
    }

    override fun getByUid(uid: UUID): SurfVisualizer? {
        return areaVisualizers.getIfPresent(uid) ?: visualizers.getIfPresent(uid)
    }
}

val visualizerApiImpl get() = SurfBukkitVisualizerApi.instance as SurfBukkitVisualizerApiImpl

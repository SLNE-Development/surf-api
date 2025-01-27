package dev.slne.surf.surfapi.bukkit.server.visualizer

import com.github.benmanes.caffeine.cache.Caffeine
import com.google.auto.service.AutoService
import dev.slne.surf.surfapi.bukkit.api.visualizer.SurfBukkitVisualizerApi
import dev.slne.surf.surfapi.bukkit.api.visualizer.visualizer.SurfVisualizer
import dev.slne.surf.surfapi.bukkit.api.visualizer.visualizer.SurfVisualizerMultipleLocations
import dev.slne.surf.surfapi.bukkit.api.visualizer.visualizer.SurfVisualizerSingleLocation
import dev.slne.surf.surfapi.bukkit.server.visualizer.visualizer.SurfVisualizerMultipleLocationsImpl
import dev.slne.surf.surfapi.bukkit.server.visualizer.visualizer.SurfVisualizerSingleLocationImpl
import org.bukkit.Location
import java.util.*

@AutoService(SurfBukkitVisualizerApi::class)
class SurfBukkitVisualizerApiImpl : SurfBukkitVisualizerApi {
    private val visualizers = Caffeine.newBuilder().softValues().build<UUID, SurfVisualizer>()

    override fun createSingleLocationVisualizer(location: Location): SurfVisualizerSingleLocation {
        return SurfVisualizerSingleLocationImpl(location).also { visualizers.put(it.uid, it) }
    }

    override fun createMultiLocationVisualizer(): SurfVisualizerMultipleLocations {
        return SurfVisualizerMultipleLocationsImpl().also { visualizers.put(it.uid, it) }
    }

    override fun getByUid(uid: UUID): SurfVisualizer? {
        return visualizers.getIfPresent(uid)
    }
}

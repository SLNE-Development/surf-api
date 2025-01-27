package dev.slne.surf.surfapi.bukkit.api.visualizer

import dev.slne.surf.surfapi.bukkit.api.visualizer.visualizer.ExperimentalVisualizerApi
import dev.slne.surf.surfapi.bukkit.api.visualizer.visualizer.SurfVisualizer
import dev.slne.surf.surfapi.bukkit.api.visualizer.visualizer.SurfVisualizerMultipleLocations
import dev.slne.surf.surfapi.bukkit.api.visualizer.visualizer.SurfVisualizerSingleLocation
import dev.slne.surf.surfapi.core.api.util.requiredService
import org.bukkit.Location
import java.util.*

@ExperimentalVisualizerApi
interface SurfBukkitVisualizerApi {

    fun createSingleLocationVisualizer(location: Location): SurfVisualizerSingleLocation
    fun createMultiLocationVisualizer(): SurfVisualizerMultipleLocations

    fun getByUid(uid: UUID): SurfVisualizer?

    companion object {
        @JvmStatic
        val instance = requiredService<SurfBukkitVisualizerApi>()
    }
}

@ExperimentalVisualizerApi
inline fun <reified T: SurfVisualizer> SurfBukkitVisualizerApi.getByUid(uid: UUID) = getByUid(uid) as? T


@ExperimentalVisualizerApi
val surfVisualizerApi get() = SurfBukkitVisualizerApi.instance

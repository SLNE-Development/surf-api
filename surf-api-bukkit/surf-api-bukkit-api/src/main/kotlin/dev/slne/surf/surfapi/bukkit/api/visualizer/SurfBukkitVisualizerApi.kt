package dev.slne.surf.surfapi.bukkit.api.visualizer

import dev.slne.surf.surfapi.bukkit.api.visualizer.visualizer.SurfPatternedVisualizer
import dev.slne.surf.surfapi.bukkit.api.visualizer.visualizer.SurfVisualizer
import dev.slne.surf.surfapi.core.api.util.requiredService

interface SurfBukkitVisualizerApi {
    fun createVisualizer(): SurfVisualizer
    fun createPatternedVisualizer(): SurfPatternedVisualizer

    companion object {
        @JvmStatic
        val instance = requiredService<SurfBukkitVisualizerApi>()
    }
}

val surfBukkitVisualizerApi get() = SurfBukkitVisualizerApi.instance

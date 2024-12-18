package dev.slne.surf.surfapi.bukkit.server.visualizer

import dev.slne.surf.surfapi.bukkit.api.visualizer.SurfBukkitVisualizerApi
import dev.slne.surf.surfapi.bukkit.api.visualizer.visualizer.SurfPatternedVisualizer
import dev.slne.surf.surfapi.bukkit.api.visualizer.visualizer.SurfVisualizer
import dev.slne.surf.surfapi.bukkit.server.visualizer.visualizer.SurfPatternedVisualizerImpl
import dev.slne.surf.surfapi.bukkit.server.visualizer.visualizer.SurfVisualizerImpl
import org.jetbrains.annotations.ApiStatus

@ApiStatus.Internal
class SurfBukkitVisualizerApiImpl : SurfBukkitVisualizerApi {
    override fun createVisualizer(): SurfVisualizer {
        return SurfVisualizerImpl()
    }

    override fun createPatternedVisualizer(): SurfPatternedVisualizer {
        return SurfPatternedVisualizerImpl()
    }
}

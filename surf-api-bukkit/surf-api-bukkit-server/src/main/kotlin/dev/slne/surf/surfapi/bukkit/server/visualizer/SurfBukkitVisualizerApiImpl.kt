package dev.slne.surf.surfapi.bukkit.server.visualizer

import com.google.auto.service.AutoService
import dev.slne.surf.surfapi.bukkit.api.visualizer.SurfBukkitVisualizerApi
import dev.slne.surf.surfapi.bukkit.api.visualizer.visualizer.SurfPatternedVisualizer
import dev.slne.surf.surfapi.bukkit.api.visualizer.visualizer.SurfVisualizer
import dev.slne.surf.surfapi.bukkit.server.visualizer.visualizer.SurfPatternedVisualizerImpl
import dev.slne.surf.surfapi.bukkit.server.visualizer.visualizer.SurfVisualizerImpl

@AutoService(SurfBukkitVisualizerApi::class)
class SurfBukkitVisualizerApiImpl : SurfBukkitVisualizerApi {
    override fun createVisualizer(): SurfVisualizer {
        return SurfVisualizerImpl()
    }

    override fun createPatternedVisualizer(): SurfPatternedVisualizer {
        return SurfPatternedVisualizerImpl()
    }
}

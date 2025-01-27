package dev.slne.surf.surfapi.bukkit.api.visualizer.visualizer

import dev.slne.surf.surfapi.bukkit.api.nms.bridges.packets.entity.BlockDisplaySettings
import org.bukkit.Location

@ExperimentalVisualizerApi
interface SurfVisualizerSingleLocation: SurfVisualizer {
    var location: Location
    var settings: BlockDisplaySettings
    fun settings(consumer: BlockDisplaySettings.() -> Unit)
}
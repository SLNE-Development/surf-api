package dev.slne.surf.surfapi.bukkit.api.visualizer.visualizer

import dev.slne.surf.surfapi.bukkit.api.nms.bridges.packets.entity.BlockDisplaySettings
import it.unimi.dsi.fastutil.objects.ObjectSet
import org.bukkit.Location
import org.jetbrains.annotations.Unmodifiable

@ExperimentalVisualizerApi
interface SurfVisualizerMultipleLocations : SurfVisualizer {
    val visualLocations: @Unmodifiable ObjectSet<Location>

    fun addVisualLocation(
        visualLocation: Location,
        consumer: BlockDisplaySettings.() -> Unit = {},
    ) = addVisualLocation(visualLocation, BlockDisplaySettings.create(consumer))

    fun addVisualLocation(
        visualLocation: Location,
        settings: BlockDisplaySettings,
    )

    fun removeVisualLocation(visualLocation: Location)
    fun clearVisualLocations()
}
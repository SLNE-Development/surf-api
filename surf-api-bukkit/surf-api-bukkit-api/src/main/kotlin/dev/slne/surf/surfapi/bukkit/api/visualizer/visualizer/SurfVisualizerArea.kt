package dev.slne.surf.surfapi.bukkit.api.visualizer.visualizer

import it.unimi.dsi.fastutil.objects.ObjectSet
import org.bukkit.Location
import org.jetbrains.annotations.Unmodifiable

@ExperimentalVisualizerApi
interface SurfVisualizerArea : SurfVisualizer {
    val cornerLocations: @Unmodifiable ObjectSet<Location>

    fun addCornerLocation(location: Location)
    fun removeCornerLocation(location: Location)
    fun clearCornerLocations()
}
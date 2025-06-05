package dev.slne.surf.surfapi.bukkit.api.visualizer.visualizer

import dev.slne.surf.surfapi.bukkit.api.nms.bridges.packets.entity.BlockDisplaySettings
import it.unimi.dsi.fastutil.objects.ObjectSet
import org.bukkit.Location
import org.jetbrains.annotations.UnmodifiableView

/**
 * Represents a specific visualizer area that extends the core functionality
 * of the `SurfVisualizer` interface by introducing corner-based location
 * management capabilities. This visualizer allows precise visualization control
 * by defining specific corner locations for rendering or delimiting areas.
 *
 * This API is experimental and subject to change. Use with caution as
 * it may contain bugs or undergo significant modifications in future updates.
 */
@ExperimentalVisualizerApi
interface SurfVisualizerArea : SurfVisualizer {

    /**
     * Represents an unmodifiable set of corner locations associated with the visualizer area.
     * These locations define the boundaries or reference points for the visualizer's area of operation.
     *
     * The set is unmodifiable, meaning any attempts to modify it directly will result in an exception.
     * Modifications can only be done through specific methods provided by the containing interface,
     * such as `addCornerLocation`, `removeCornerLocation`, or `clearCornerLocations`.
     *
     * This property is part of the experimental API and may be subject to changes in the future.
     */
    val cornerLocations: @UnmodifiableView ObjectSet<Location>

    /**
     * Adds a location to the set of corner locations in the visualizer area.
     *
     * @param location The location to be added as a corner point for defining the visualizer area.
     */
    fun addCornerLocation(location: Location)

    /**
     * Removes a specific location from the set of corner locations in the visualizer area.
     *
     * @param location The location to be removed from the corner locations.
     */
    fun removeCornerLocation(location: Location)

    /**
     * Clears all currently registered corner locations associated with the visualizer area.
     *
     * This method removes all entries from the internal collection of corner locations,
     * effectively resetting the state of corners for the current visualizer instance.
     *
     * Once called, the visualizer will no longer have any corner locations defined
     * until new ones are explicitly added.
     */
    fun clearCornerLocations()

    fun setCornerLocations(locations: Collection<Location>)

    var settings: BlockDisplaySettings
    fun settings(consumer: BlockDisplaySettings.() -> Unit)
}
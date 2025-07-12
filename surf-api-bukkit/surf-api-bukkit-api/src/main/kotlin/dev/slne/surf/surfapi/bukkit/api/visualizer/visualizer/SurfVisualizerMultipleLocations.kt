package dev.slne.surf.surfapi.bukkit.api.visualizer.visualizer

import dev.slne.surf.surfapi.bukkit.api.nms.bridges.packets.entity.BlockDisplaySettings
import it.unimi.dsi.fastutil.objects.ObjectSet
import org.jetbrains.annotations.UnmodifiableView
import org.spongepowered.math.vector.Vector3d

/**
 * Represents a specialized visualizer interface that supports managing and displaying
 * visualizations across multiple locations simultaneously. It extends the base functionality
 * of the SurfVisualizer interface, allowing the addition, removal, and management of
 * visual locations along with associated display settings.
 *
 * This interface is part of an experimental API and may be subject to changes in the
 * future.
 */
@ExperimentalVisualizerApi
interface SurfVisualizerMultipleLocations : SurfVisualizer {
    /**
     * Represents an unmodifiable set of locations associated with the visualizer.
     * The locations within this set are used to define specific points for visualization purposes.
     *
     * This collection cannot be modified directly and is intended to be managed
     * through appropriate methods provided in the respective visualizer interface.
     *
     * Part of the experimental `SurfVisualizerMultipleLocations` API, which is subject to change.
     */
    val visualLocations: @UnmodifiableView ObjectSet<Vector3d>

    /**
     * Adds a visual location to the visualizer with optional display settings configuration.
     * This method allows associating a new `Location` object with the visualizer while applying
     * customized display settings using the provided consumer function.
     *
     * @param visualLocation The location to be added for visualization.
     * @param consumer A lambda for configuring display settings using `BlockDisplaySettings`. Defaults to an empty lambda.
     */
    fun addVisualLocation(
        visualLocation: Vector3d,
        consumer: BlockDisplaySettings.() -> Unit = {},
    ) = addVisualLocation(visualLocation, BlockDisplaySettings.create(consumer))

    /**
     * Adds a visual location with the specified settings to the visualizer.
     *
     * @param visualLocation The location to be added as a visual point.
     * @param settings The display settings to be used for the visual representation at the specified location.
     */
    fun addVisualLocation(
        visualLocation: Vector3d,
        settings: BlockDisplaySettings,
    )

    /**
     * Removes the specified visual location from the current visualizer.
     *
     * @param visualLocation The `Location` instance representing the visual location to be removed.
     */
    fun removeVisualLocation(visualLocation: Vector3d)
    /**
     * Clears all visual locations currently associated with this visualizer.
     *
     * This method removes all the visual locations from the internal storage,
     * effectively resetting the visualized locations for this instance.
     * After calling this method, no visual locations will remain until new ones
     * are explicitly added.
     *
     * Typically used when resetting or reconfiguring the visualizer's state, or
     * when performing cleanup operations.
     */
    fun clearVisualLocations()
}
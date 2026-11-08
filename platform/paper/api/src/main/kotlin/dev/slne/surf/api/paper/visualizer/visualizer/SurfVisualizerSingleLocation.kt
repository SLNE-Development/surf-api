package dev.slne.surf.api.paper.visualizer.visualizer

import dev.slne.surf.api.paper.nms.bridges.packets.entity.BlockDisplaySettings
import org.bukkit.Location

/**
 * Represents a surf visualizer specific to a single location.
 * This interface extends the functionality of the base SurfVisualizer interface,
 * enabling visualization anchored to a single [location].
 *
 * The visualizer is designed to operate within the Bukkit environment and
 * provides customization settings for the visual representation using
 * [BlockDisplaySettings].
 *
 * Note: This interface is part of an experimental API and may be subject to changes.
 */
@ExperimentalVisualizerApi
interface SurfVisualizerSingleLocation : SurfVisualizer {
    /**
     * Represents the primary location used by the surf visualizer to establish
     * a fixed or centralized point of visualization.
     *
     * The precise role and behavior of this location depend on the specific implementation
     * and configuration of the visualizer. In some cases, it could denote a target position
     * for rendering or visualization, while in others, it might represent an anchor point
     * for a larger area or structure being visualized.
     *
     * As part of an experimental API, this property is subject to future changes or refinements.
     */
    var location: Location

    /**
     * Represents the block display settings associated with the visualizer.
     *
     * This variable holds configuration details for visualizing block data in
     * a specific display context. You can use a consumer function or direct manipulation
     * of this object to update visualization settings.
     *
     * Modifications to this property should be done cautiously as this is part
     * of an experimental API and is subject to breaking changes in future updates.
     */
    var settings: BlockDisplaySettings

    /**
     * Configures the `BlockDisplaySettings` using the provided lambda.
     * Allows customization of display settings for a block within the visualizer.
     *
     * @param consumer A lambda with a receiver of `BlockDisplaySettings`, used to define or modify the settings.
     */
    fun settings(consumer: BlockDisplaySettings.() -> Unit)
}
package dev.slne.surf.surfapi.bukkit.api.visualizer.visualizer

/**
 * Defines the strategies available for updating a visualizer within the
 * experimental visualizer API. These strategies specify which aspects of
 * the visualizer should be updated when changes occur.
 *
 * This enum is part of an experimental API and may be subject to changes,
 * including new strategy options or modifications to existing ones.
 */
@ExperimentalVisualizerApi
enum class UpdateStrategy {
    /**
     * Represents an update strategy where all aspects of the visualizer
     * are updated simultaneously. This includes both the position updates
     * and any other properties or states associated with the visualizer.
     *
     * Usage of this strategy can be beneficial when a comprehensive
     * refresh of the visualizer's state is required.
     */
    ALL,
    /**
     * Indicates that the visualizer update should target only position-specific properties.
     *
     * This update strategy is part of an experimental API and may be subject to changes.
     * It is typically used when the visualizer requires modifications or adjustments
     * limited to its positional aspects, without affecting other visualizer attributes.
     *
     * POSITION ensures efficient updates by focusing solely on position-related features,
     * avoiding unnecessary recalculations or modifications of other components.
     */
    POSITION,
}
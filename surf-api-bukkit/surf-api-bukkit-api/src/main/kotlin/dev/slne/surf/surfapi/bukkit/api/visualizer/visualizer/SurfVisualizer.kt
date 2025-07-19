package dev.slne.surf.surfapi.bukkit.api.visualizer.visualizer

import it.unimi.dsi.fastutil.objects.ObjectSet
import org.bukkit.block.BlockType
import org.bukkit.entity.Player
import org.jetbrains.annotations.UnmodifiableView
import java.util.*


/**
 * Represents a core visualizer interface that provides functionality
 * for starting, stopping, and managing visualization within a Bukkit environment.
 * This interface acts as a base for other specific visualizer types.
 *
 * All methods and properties defined here are part of an experimental API
 * and may be subject to changes in future updates.
 */
@ExperimentalVisualizerApi
interface SurfVisualizer {
    /**
     * A unique identifier for the visualizer instance.
     * This identifier is used to distinguish one visualizer object from another.
     */
    val uid: UUID

    /**
     * Initiates the visualization process for the visualizer instance.
     *
     * @return `true` if the visualization started successfully, `false` otherwise.
     */
    fun startVisualizing(): Boolean

    /**
     * Stops the visualization process for the current visualizer.
     *
     * @return `true` if the visualizer was successfully stopped, or `false` if it was not running.
     */
    fun stopVisualizing(): Boolean

    /**
     * Determines whether the visualizer is currently in a visualizing state.
     *
     * @return true if the visualizer is actively visualizing, false otherwise.
     */
    fun isVisualizing(): Boolean

    /**
     * Represents a set of players currently viewing the visualizer.
     * This set is unmodifiable and automatically reflects any changes made
     * through the `addViewer`, `removeViewer`, or `clearViewers` methods.
     *
     * Modifications to the set directly are not allowed, ensuring consistency
     * with the visualizer's state.
     */
    val viewers: @UnmodifiableView ObjectSet<Player>

    /**
     * Adds a specified player to the list of viewers for this visualizer.
     *
     * @param player The player to be added as a viewer.
     */
    fun addViewer(player: Player)

    /**
     * Removes a viewer from the visualizer viewers list.
     *
     * @param player The player to be removed as a viewer.
     */
    fun removeViewer(player: Player)

    /**
     * Clears all players from the list of viewers watching the visualizer.
     *
     * This method removes all currently registered viewers from the internal collection
     * of viewers associated with the visualizer instance. After calling this method, the
     * visualizer will not be visible to any previously registered viewers.
     *
     * It is commonly used when resetting or disabling the visualizer, or when reassigning
     * visibility states for viewers.
     */
    fun clearViewers()

    /**
     * Checks whether there are any viewers currently associated with the visualizer.
     *
     * @return true if there is at least one viewer, false otherwise
     */
    fun hasViewers(): Boolean

    /**
     * Determines whether the visualizer is visible to the specified player.
     *
     * @param player The player for whom the visibility of the visualizer is to be checked.
     * @return `true` if the visualizer is visible to the specified player, `false` otherwise.
     */
    fun visibleTo(player: Player): Boolean

    /**
     * Updates the visualizer using the specified update strategy.
     *
     * @param strategy defines the update strategy to use. Defaults to [UpdateStrategy.ALL],
     * which updates all aspects of the visualizer. Other strategies may target specific
     * aspects like position updates.
     */
    fun update(strategy: UpdateStrategy = UpdateStrategy.ALL)

    /**
     * Companion object for the `SurfVisualizer` interface.
     * Provides shared constants and utilities related to visualizer functionality.
     */
    companion object {
        /**
         * Represents the default material used by instances of a visualizer.
         * This material is primarily used as a fallback or initial setting,
         * typically for rendering or visualizing purposes.
         *
         * This constant is part of the companion object of the `SurfVisualizer`
         * interface, allowing consistent use across all implementations.
         */
        @JvmField
        val DEFAULT_BLOCK_TYPE: BlockType = BlockType.GLASS
    }
}

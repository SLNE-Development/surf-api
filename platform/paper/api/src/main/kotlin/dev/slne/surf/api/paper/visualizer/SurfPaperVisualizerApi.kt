@file:OptIn(InternalSurfApi::class)

package dev.slne.surf.api.paper.visualizer

import dev.slne.surf.api.paper.nms.bridges.packets.entity.BlockDisplaySettings
import dev.slne.surf.api.paper.visualizer.visualizer.*
import dev.slne.surf.api.shared.api.util.InternalSurfApi
import dev.slne.surf.api.spring.SurfApiBean
import dev.slne.surf.api.spring.surfBean
import org.bukkit.Location
import org.bukkit.World
import org.spongepowered.math.vector.Vector3d
import java.util.*
import kotlin.time.Duration

/**
 * Defines the main API for working with visualizers in the SurfBukkit visualizer system.
 * This API allows the creation and management of single-location and multi-location visualizers,
 * as well as retrieval of visualizers by their unique identifier.
 *
 * This interface is marked as experimental and may be subject to changes in future updates.
 */
@ExperimentalVisualizerApi
@SurfApiBean
interface SurfPaperVisualizerApi {

    /**
     * Creates a visualizer for a single fixed location.
     *
     * The visualizer enables visualization functionalities anchored to the given location
     * and allows configuration of block display settings or other visualization parameters.
     * This method is a part of the experimental API and should be used cautiously
     * as it may change in future releases.
     *
     * @param location The location at which the visualizer will be anchored.
     * @return An instance of SurfVisualizerSingleLocation representing the visualizer created for the specified location.
     */
    fun createSingleLocationVisualizer(location: Location): SurfVisualizerSingleLocation

    /**
     * Creates a new instance of `SurfVisualizerMultipleLocations`, which supports managing visualizations
     * across multiple locations. This visualizer allows adding, removing, and managing specific visual
     * locations along with their associated display settings.
     *
     * This method is part of an experimental API and may be subject to changes in future updates.
     *
     * @return A new `SurfVisualizerMultipleLocations` instance that provides functionality to handle
     *         multiple visualization locations.
     */
    fun createMultiLocationVisualizer(world: World): SurfVisualizerMultipleLocations

    fun createAreaVisualizer(
        world: World,
        initialSettings: BlockDisplaySettings? = null,
        initialEdges: Collection<Vector3d> = emptyList(),
        useHighestYBlock: Boolean = false,
        placeDelay: Duration = Duration.ZERO,
    ): SurfVisualizerArea

    /**
     * Retrieves a `SurfVisualizer` instance by its unique identifier.
     *
     * This function searches for and returns a `SurfVisualizer` object associated with the provided UUID.
     * If no visualizer is found with the given identifier, the function returns `null`.
     *
     * @param uid The unique identifier of the visualizer to be retrieved.
     * @return The `SurfVisualizer` instance associated with the provided UUID, or `null` if no match is found.
     */
    fun getByUid(uid: UUID): SurfVisualizer?

    /**
     * Static access to the [SurfPaperVisualizerApi] bean for surf-api's own entry points.
     */
    companion object {
        /**
         * The [SurfPaperVisualizerApi] bean of surf-api's root Spring context, resolved on first use.
         *
         * @throws org.springframework.beans.factory.NoSuchBeanDefinitionException if the root context
         * provides no such bean
         */
        @InternalSurfApi
        val instance by surfBean<SurfPaperVisualizerApi>()
    }
}

/**
 * Retrieves a `SurfVisualizer` instance of the specified type `T` corresponding to the given unique identifier (UID).
 * If the visualizer with the provided UID exists and is of type `T`, it will be returned; otherwise, `null` will be returned.
 *
 * @param uid The unique identifier of the visualizer to retrieve.
 * @return The visualizer instance of type `T` matching the given UID, or `null` if no matching instance is found.
 */
@ExperimentalVisualizerApi
inline fun <reified T : SurfVisualizer> SurfPaperVisualizerApi.getByUid(uid: UUID) =
    getByUid(uid) as? T
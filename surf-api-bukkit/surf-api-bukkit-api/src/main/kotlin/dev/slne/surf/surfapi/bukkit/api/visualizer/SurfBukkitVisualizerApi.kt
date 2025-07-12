package dev.slne.surf.surfapi.bukkit.api.visualizer

import dev.slne.surf.surfapi.bukkit.api.nms.bridges.packets.entity.BlockDisplaySettings
import dev.slne.surf.surfapi.bukkit.api.visualizer.visualizer.ExperimentalVisualizerApi
import dev.slne.surf.surfapi.bukkit.api.visualizer.visualizer.SurfVisualizer
import dev.slne.surf.surfapi.bukkit.api.visualizer.visualizer.SurfVisualizerArea
import dev.slne.surf.surfapi.bukkit.api.visualizer.visualizer.SurfVisualizerMultipleLocations
import dev.slne.surf.surfapi.bukkit.api.visualizer.visualizer.SurfVisualizerSingleLocation
import dev.slne.surf.surfapi.core.api.util.requiredService
import org.bukkit.Location
import org.bukkit.World
import org.spongepowered.math.vector.Vector3d
import java.util.*

/**
 * Defines the main API for working with visualizers in the SurfBukkit visualizer system.
 * This API allows the creation and management of single-location and multi-location visualizers,
 * as well as retrieval of visualizers by their unique identifier.
 *
 * This interface is marked as experimental and may be subject to changes in future updates.
 */
@ExperimentalVisualizerApi
interface SurfBukkitVisualizerApi {

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
     * Provides a globally accessible singleton instance of `SurfBukkitVisualizerApi`.
     *
     * This companion object allows static access to the `SurfBukkitVisualizerApi` service,
     * enabling users to retrieve the required visualizer API implementation. The `instance` property
     * is initialized using the `requiredService` function, ensuring that the service is available
     * and properly configured. If the service is unavailable, an error is thrown.
     *
     * The `SurfBukkitVisualizerApi` interface and its associated features are marked as experimental,
     * and users must opt into this API with caution, as it may be subject to changes in future versions.
     */
    companion object {
        /**
         * Singleton instance of the `SurfBukkitVisualizerApi` service.
         *
         * This property provides access to the required implementation of the `SurfBukkitVisualizerApi` interface.
         * It is retrieved using the `requiredService` function, which ensures that the service is available.
         * If the service is not available, an `Error` will be thrown.
         *
         * The `SurfBukkitVisualizerApi` interface is experimental and provides access to the creation and management
         * of visualizers within the Surf framework in a Bukkit environment. The API is annotated with
         * `@ExperimentalVisualizerApi`, indicating its experimental nature and potential for future changes.
         *
         * This property is annotated with `@JvmStatic`, making it accessible directly at the class level in Java.
         */
        @JvmStatic
        val instance = requiredService<SurfBukkitVisualizerApi>()
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
inline fun <reified T : SurfVisualizer> SurfBukkitVisualizerApi.getByUid(uid: UUID) =
    getByUid(uid) as? T


/**
 * Provides access to the singleton instance of the `SurfBukkitVisualizerApi`,
 * which serves as the entry point for managing and interacting with Bukkit-based
 * visualizer functionality in the Surf API framework.
 *
 * This API is experimental and may be subject to change. Users should opt-in
 * to its usage with the understanding that it could contain bugs or undergo
 * significant updates in future versions.
 *
 * The `SurfBukkitVisualizerApi` instance allows for initializing, updating,
 * and managing visualization elements within the Bukkit environment. It is
 * intended for use in advanced visualization scenarios, including but not
 * limited to player-specific views, area definitions, and custom rendering strategies.
 *
 * Utilization of this property requires careful consideration of the
 * experimental nature of the API.
 *
 * @see SurfVisualizer
 * @see SurfVisualizerArea
 * @see UpdateStrategy
 */
@ExperimentalVisualizerApi
val surfVisualizerApi get() = SurfBukkitVisualizerApi.instance
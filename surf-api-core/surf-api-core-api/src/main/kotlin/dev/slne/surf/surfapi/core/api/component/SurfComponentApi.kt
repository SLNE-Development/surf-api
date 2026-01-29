package dev.slne.surf.surfapi.core.api.component

import dev.slne.surf.surfapi.core.api.util.requiredService
import dev.slne.surf.surfapi.shared.api.component.Component

/**
 * Main API for managing the component lifecycle.
 *
 * This API provides methods to trigger lifecycle phases for components
 * and to query loaded components. Components are loaded lazily when
 * their lifecycle methods are first called.
 *
 * Typical usage in a plugin:
 * ```kotlin
 * class MyPlugin : JavaPlugin() {
 *     override fun onLoad() {
 *         runBlocking { surfComponentApi.load(this@MyPlugin) }
 *     }
 *
 *     override fun onEnable() {
 *         runBlocking { surfComponentApi.enable(this@MyPlugin) }
 *     }
 *
 *     override fun onDisable() {
 *         runBlocking { surfComponentApi.disable(this@MyPlugin) }
 *     }
 * }
 * ```
 *
 * @see Component
 * @see AbstractComponent
 */
interface SurfComponentApi {

    /**
     * Triggers the bootstrap phase for all components owned by the given owner.
     * Components are loaded lazily if not already loaded.
     *
     * @param owner The owner of the components (typically a plugin instance)
     */
    suspend fun bootstrap(owner: Any)

    /**
     * Triggers the load phase for all components owned by the given owner.
     * This also triggers the bootstrap phase if not already done.
     *
     * @param owner The owner of the components (typically a plugin instance)
     */
    suspend fun load(owner: Any)

    /**
     * Triggers the enable phase for all components owned by the given owner.
     * This also triggers the load phase if not already done.
     *
     * @param owner The owner of the components (typically a plugin instance)
     */
    suspend fun enable(owner: Any)

    /**
     * Triggers the disable phase for all components owned by the given owner.
     * Components are disabled in reverse order of their initialization.
     * Post-processor destruction callbacks are invoked before disabling.
     *
     * @param owner The owner of the components (typically a plugin instance)
     */
    suspend fun disable(owner: Any)

    /**
     * Returns all components of the specified type for the given owner.
     * Components are loaded lazily if not already loaded.
     *
     * @param owner The owner of the components
     * @param type The class of components to filter for
     * @return List of components matching the specified type
     */
    suspend fun <T : Any> componentsOfType(owner: Any, type: Class<T>): List<T>

    /**
     * Returns all already-loaded components of the specified type for the given owner.
     * Does not trigger lazy loading.
     *
     * @param owner The owner of the components
     * @param type The class of components to filter for
     * @return List of loaded components matching the specified type
     */
    fun <T : Any> componentsOfTypeLoaded(owner: Any, type: Class<T>): List<T>

    /**
     * Returns all components of the specified type across all owners.
     *
     * @param type The class of components to filter for
     * @return List of components matching the specified type
     */
    suspend fun <T : Any> componentsOfType(type: Class<T>): List<T>

    /**
     * Returns all already-loaded components of the specified type across all owners.
     *
     * @param type The class of components to filter for
     * @return List of loaded components matching the specified type
     */
    fun <T : Any> componentsOfTypeLoaded(type: Class<T>): List<T>

    /**
     * Returns all components for the given owner.
     * Components are loaded lazily if not already loaded.
     *
     * @param owner The owner of the components
     * @return List of all components for the owner
     */
    suspend fun components(owner: Any): List<Component>

    /**
     * Returns all already-loaded components for the given owner.
     * Does not trigger lazy loading.
     *
     * @param owner The owner of the components
     * @return List of loaded components for the owner
     */
    fun componentsLoaded(owner: Any): List<Component>

    companion object {
        /**
         * The singleton instance of the component API.
         */
        val instance = requiredService<SurfComponentApi>()
    }
}

/**
 * Convenience property to access the [SurfComponentApi] instance.
 */
val surfComponentApi get() = SurfComponentApi.instance

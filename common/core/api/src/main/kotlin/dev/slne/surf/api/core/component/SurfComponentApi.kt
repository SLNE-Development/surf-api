package dev.slne.surf.api.core.component

import dev.slne.surf.api.core.component.SurfComponentApi.Companion.load
import dev.slne.surf.api.core.util.requiredService
import dev.slne.surf.api.shared.api.component.SurfComponent

/**
 * Main API for managing the component lifecycle.
 *
 * This API provides methods to trigger lifecycle phases for components
 * and to query loaded components. Components are loaded lazily when
 * their lifecycle methods are first called.
 *
 * Phase chaining behavior:
 * - If a component **extends `AbstractComponent`**, calling a later phase will automatically invoke
 *   the required previous phases (e.g. `enable()` will call `load()` first if needed).
 * - If a component **only implements `Component`** (and does **not** extend `AbstractComponent`),
 *   phases are **not** automatically chained. You must call the phases in order yourself.
 *
 * Typical usage in a plugin:
 * ```kotlin
 * class MyPlugin : SuspendingJavaPlugin() {
 *     override suspend fun onLoadAsync() {
 *         surfComponentApi.load(this)
 *     }
 *
 *     override suspend fun onEnableAsync() {
 *         surfComponentApi.enable(this)
 *     }
 *
 *     override suspend fun onDisableAsync() {
 *         surfComponentApi.disable(this)
 *     }
 * }
 * ```
 *
 * @see Component
 * @see AbstractComponent
 */
interface SurfComponentApi {

    /**
     * Triggers the load phase for all components owned by the given owner.
     * Components are loaded lazily if not already loaded.
     *
     * @param owner The owner of the components (a plugin instance)
     */
    suspend fun load(owner: Any)

    /**
     * Triggers the enable phase for all components owned by the given owner.
     *
     * If the component extends [AbstractComponent], this also triggers the load phase
     * if not already done.
     *
     * If the component only implements [Component] (and does not extend [AbstractComponent]),
     * this method does **not** automatically call [load]; you must call it yourself
     * in the correct order.
     *
     * @param owner The owner of the components (a plugin instance)
     */
    suspend fun enable(owner: Any)

    /**
     * Triggers the disable phase for all components owned by the given owner.
     * Components are disabled in reverse order of their initialization.
     * Post-processor destruction callbacks are invoked before disabling.
     *
     * @param owner The owner of the components (a plugin instance)
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
    suspend fun components(owner: Any): List<SurfComponent>

    /**
     * Returns all already-loaded components for the given owner.
     * Does not trigger lazy loading.
     *
     * @param owner The owner of the components
     * @return List of loaded components for the owner
     */
    fun componentsLoaded(owner: Any): List<SurfComponent>

    companion object : SurfComponentApi by surfComponentApi {
        val instance = surfComponentApi
    }
}

val surfComponentApi = requiredService<SurfComponentApi>()

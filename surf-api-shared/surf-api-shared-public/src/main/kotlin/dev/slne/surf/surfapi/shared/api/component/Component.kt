package dev.slne.surf.surfapi.shared.api.component

/**
 * Core interface for all components in the component system.
 *
 * Components are modular units of functionality that follow a defined lifecycle.
 * They are automatically discovered at compile time and instantiated at runtime
 * based on their dependencies and conditions.
 *
 * The lifecycle of a component consists of four phases:
 * 1. [bootstrap] - Initial bootstrap phase, called first
 * 2. [load] - Loading phase, called after bootstrap
 * 3. [enable] - Enable phase, called after load
 * 4. [disable] - Disable phase, called during shutdown
 *
 * Components are sorted by [priority] and dependency order. Lower priority values
 * are initialized first. Dependencies declared via [@DependsOnComponent][dev.slne.surf.surfapi.shared.api.component.requirement.DependsOnComponent]
 * are guaranteed to be initialized before their dependents.
 *
 * @see AbstractComponent
 * @see ComponentMeta
 */
interface Component : Comparable<Component> {
    /**
     * The priority of this component for ordering purposes.
     * Lower values indicate higher priority (initialized first).
     * Components with the same priority are ordered by their dependencies.
     */
    val priority: Short

    /**
     * Called during the bootstrap phase.
     * This is the first lifecycle method called on the component.
     * Use this for early initialization that doesn't depend on other components.
     */
    suspend fun bootstrap()

    /**
     * Called during the load phase, after [bootstrap].
     * Use this for loading configuration, resources, or other data.
     */
    suspend fun load()

    /**
     * Called during the enable phase, after [load].
     * Use this to activate the component's functionality.
     */
    suspend fun enable()

    /**
     * Called during the disable phase, during shutdown.
     * Use this to clean up resources and deactivate functionality.
     * Components are disabled in reverse order of their initialization.
     */
    suspend fun disable()
}

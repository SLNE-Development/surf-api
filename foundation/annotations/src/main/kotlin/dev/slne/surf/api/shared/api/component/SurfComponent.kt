package dev.slne.surf.api.shared.api.component

import org.jetbrains.annotations.ApiStatus

/**
 * Core interface for all components in the component system.
 *
 * Components are modular units of functionality that follow a defined lifecycle.
 * They are automatically discovered at compile time and instantiated at runtime
 * based on their dependencies and conditions.
 *
 * The lifecycle of a component consists of three phases:
 * 1. [load] - Initial loading phase, called first
 * 2. [enable] - Enable phase, called after load
 * 3. [disable] - Disable phase, called during shutdown
 *
 * Components are sorted by their [@Priority][Priority] (if present) and dependency order. Lower priority values
 * are initialized first. Dependencies declared via [@DependsOnComponent][dev.slne.surf.api.shared.api.component.requirement.DependsOnComponent]
 * are guaranteed to be initialized before their dependents.
 *
 * @see AbstractComponent
 * @see SurfComponentMeta
 */
interface SurfComponent {
    /**
     * Called during the load phase.
     * This is the first lifecycle method called on the component.
     * Use this for loading configuration, resources, or other data.
     */
    @ApiStatus.OverrideOnly
    suspend fun load() = Unit

    /**
     * Called during the enable phase, after [load].
     * Use this to activate the component's functionality.
     */
    @ApiStatus.OverrideOnly
    suspend fun enable() = Unit

    /**
     * Called during the disable phase, during shutdown.
     * Use this to clean up resources and deactivate functionality.
     * Components are disabled in reverse order of their initialization.
     */
    @ApiStatus.OverrideOnly
    suspend fun disable() = Unit
}
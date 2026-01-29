package dev.slne.surf.surfapi.shared.api.component.processor

import dev.slne.surf.surfapi.shared.api.component.Component

/**
 * Interface for post-processing components after initialization.
 * 
 * Implementations are auto-discovered by the symbol processor without requiring any annotation.
 * Simply implement this interface and the processor will register it automatically.
 * 
 * Post-processors are executed in the following order:
 * 1. Component instantiation
 * 2. For each post-processor (sorted by priority, lower first): postProcessAfterInitialization()
 * 3. Component.bootstrap()
 * 4. Component.load()
 * 5. Component.enable()
 * 6. ...
 * 7. For each post-processor (reverse priority order): postProcessBeforeDestruction()
 * 8. Component.disable()
 *
 * Example usage:
 * ```kotlin
 * class LoggingPostProcessor : ComponentPostProcessor {
 *     override val priority: Int = 10
 *
 *     override suspend fun postProcessAfterInitialization(
 *         component: Component,
 *         componentName: String,
 *         context: ComponentContext
 *     ): Component {
 *         println("Initialized: $componentName")
 *         return component
 *     }
 * }
 * ```
 */
interface ComponentPostProcessor {
    /**
     * Priority for ordering multiple processors.
     * Lower values are executed first during initialization.
     * During destruction, the order is reversed (higher values first).
     * Default is 0.
     */
    val priority: Int get() = 0

    /**
     * Called after component initialization but before the component's bootstrap phase.
     * 
     * This method can be used for:
     * - Logging component initialization
     * - Injecting dependencies
     * - Wrapping or proxying the component
     * - Validating component configuration
     *
     * @param component The component that was just initialized
     * @param componentName The fully qualified class name of the component
     * @param context Context containing the owner, all components, and environment
     * @return The component (possibly wrapped or modified). Return the same component if no modification is needed.
     */
    suspend fun postProcessAfterInitialization(
        component: Component,
        componentName: String,
        context: ComponentContext
    ): Component

    /**
     * Called before component shutdown (before the component's disable phase).
     * 
     * This method can be used for:
     * - Logging component destruction
     * - Cleanup of resources injected during initialization
     * - Unwrapping proxied components
     *
     * The default implementation does nothing. Override only if cleanup is needed.
     *
     * @param component The component that is about to be destroyed
     * @param componentName The fully qualified class name of the component
     * @param context Context containing the owner, all components, and environment
     */
    suspend fun postProcessBeforeDestruction(
        component: Component,
        componentName: String,
        context: ComponentContext
    ) {
        // default empty - override if cleanup is needed
    }
}

package dev.slne.surf.surfapi.shared.api.component.processor

import dev.slne.surf.surfapi.shared.api.component.Component

/**
 * Interface for post-processing components after initialization.
 * Implementations are auto-discovered by the symbol processor.
 */
interface ComponentPostProcessor {
    /**
     * Priority for ordering multiple processors (lower = earlier)
     */
    val priority: Int get() = 0

    /**
     * Called after component initialization but before load phase
     */
    suspend fun postProcessAfterInitialization(
        component: Component,
        componentName: String,
        context: ComponentContext
    ): Component

    /**
     * Called before component shutdown
     */
    suspend fun postProcessBeforeDestruction(
        component: Component,
        componentName: String,
        context: ComponentContext
    ) {
        // default empty
    }
}

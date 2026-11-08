package dev.slne.surf.api.shared.api.component.processor

import dev.slne.surf.api.shared.api.component.SurfComponent

/**
 * Context object passed to [ComponentPostProcessor] methods providing information
 * about the processing environment.
 *
 * @property owner The owner of the components (e.g., the plugin instance)
 * @property allComponents List of all components that have been instantiated.
 *                         Note: During postProcessAfterInitialization, this list
 *                         contains the original components before any post-processing.
 * @property environment Additional environment variables or configuration that
 *                        can be used by post-processors
 *
 * @see ComponentPostProcessor
 */
@JvmRecord
data class ComponentContext(
    val owner: Any,
    val allComponents: List<SurfComponent>,
    val environment: Map<String, Any> = emptyMap()
)
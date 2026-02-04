package dev.slne.surf.surfapi.shared.api.component.condition

import dev.slne.surf.surfapi.shared.api.component.SurfComponent
import net.kyori.adventure.text.logger.slf4j.ComponentLogger

/**
 * Context object passed to [ComponentCondition.evaluate] providing information
 * about the component being evaluated and its environment.
 *
 * @property owner The owner of the component (e.g., the plugin instance)
 * @property componentClass The class of the component being evaluated
 * @property logger A logger that can be used for diagnostic output
 * @property environment Additional environment variables or configuration
 *
 * @see ComponentCondition
 */
@JvmRecord
data class ComponentConditionContext(
    val owner: Any,
    val componentClass: Class<out SurfComponent>,
    val logger: ComponentLogger,
    val environment: Map<String, Any> = emptyMap()
)
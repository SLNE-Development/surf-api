package dev.slne.surf.surfapi.shared.api.component.condition

import dev.slne.surf.surfapi.shared.api.component.Component
import net.kyori.adventure.text.logger.slf4j.ComponentLogger

@JvmRecord
data class ComponentConditionContext(
    val owner: Any,
    val componentClass: Class<out Component>,
    val logger: ComponentLogger,
    val environment: Map<String, Any> = emptyMap()
)

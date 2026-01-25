package dev.slne.surf.surfapi.shared.api.hook.condition

import dev.slne.surf.surfapi.shared.api.hook.Hook
import net.kyori.adventure.text.logger.slf4j.ComponentLogger

data class HookConditionContext(
    val owner: Any,
    val hookClass: Class<out Hook>,
    val logger: ComponentLogger,
    val environment: Map<String, Any> = emptyMap()
)
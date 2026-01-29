package dev.slne.surf.surfapi.shared.api.component.processor

import dev.slne.surf.surfapi.shared.api.component.Component

data class ComponentContext(
    val owner: Any,
    val allComponents: List<Component>,
    val environment: Map<String, Any> = emptyMap()
)

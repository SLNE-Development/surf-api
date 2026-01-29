package dev.slne.surf.surfapi.shared.api.component.condition

interface ComponentCondition {
    suspend fun evaluate(context: ComponentConditionContext): Boolean
}

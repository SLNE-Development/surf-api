package dev.slne.surf.surfapi.shared.api.hook.condition

interface HookCondition {
    suspend fun evaluate(context: HookConditionContext): Boolean
}
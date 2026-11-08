package dev.slne.surf.surfapi.bukkit.test.hook.condition

import dev.slne.surf.api.shared.api.component.condition.ComponentCondition
import dev.slne.surf.api.shared.api.component.condition.ComponentConditionContext
import dev.slne.surf.surfapi.bukkit.test.config.ModernTestConfig

class EnabledCondition : ComponentCondition {
    override suspend fun evaluate(context: ComponentConditionContext): Boolean {
        return ModernTestConfig.getConfig().enabled
    }
}
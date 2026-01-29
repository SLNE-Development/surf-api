package dev.slne.surf.surfapi.bukkit.test.component.condition

import dev.slne.surf.surfapi.bukkit.test.config.ModernTestConfig
import dev.slne.surf.surfapi.shared.api.component.condition.ComponentCondition
import dev.slne.surf.surfapi.shared.api.component.condition.ComponentConditionContext

class EnabledCondition : ComponentCondition {
    override suspend fun evaluate(context: ComponentConditionContext): Boolean {
        return ModernTestConfig.getConfig().enabled
    }
}

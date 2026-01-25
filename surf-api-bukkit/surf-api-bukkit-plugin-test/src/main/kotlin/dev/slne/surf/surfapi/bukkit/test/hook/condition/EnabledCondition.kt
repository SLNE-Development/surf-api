package dev.slne.surf.surfapi.bukkit.test.hook.condition

import dev.slne.surf.surfapi.bukkit.test.config.ModernTestConfig
import dev.slne.surf.surfapi.shared.api.hook.condition.HookCondition
import dev.slne.surf.surfapi.shared.api.hook.condition.HookConditionContext

class EnabledCondition : HookCondition {
    override suspend fun evaluate(context: HookConditionContext): Boolean {
        return ModernTestConfig.getConfig().enabled
    }
}
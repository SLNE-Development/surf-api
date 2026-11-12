package dev.slne.surf.surfapi.bukkit.test.hook.condition

import dev.slne.surf.api.di.condition.BeanCondition
import dev.slne.surf.api.di.condition.BeanConditionContext
import dev.slne.surf.surfapi.bukkit.test.config.ModernTestConfig

/** Lets a bean exist only while this plugin's own configuration says so. */
class EnabledCondition : BeanCondition {
    override suspend fun evaluate(context: BeanConditionContext): Boolean {
        return ModernTestConfig.getConfig().enabled
    }
}

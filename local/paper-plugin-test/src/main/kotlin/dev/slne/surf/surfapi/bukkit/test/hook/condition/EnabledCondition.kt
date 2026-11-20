package dev.slne.surf.surfapi.bukkit.test.hook.condition

import dev.slne.surf.surfapi.bukkit.test.config.ModernTestConfig
import org.springframework.context.annotation.Condition
import org.springframework.context.annotation.ConditionContext
import org.springframework.core.type.AnnotatedTypeMetadata

/**
 * Matches when the test config enables the guarded bean.
 */
class EnabledCondition : Condition {

    override fun matches(context: ConditionContext, metadata: AnnotatedTypeMetadata) =
        ModernTestConfig.getConfig().enabled
}

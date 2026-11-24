package dev.slne.surf.surfapi.bukkit.test.hook.condition

import org.springframework.context.annotation.Condition
import org.springframework.context.annotation.ConditionContext
import org.springframework.core.type.AnnotatedTypeMetadata

/**
 * Matches when the environment sets `test.property` to `true`, for example with
 * `-Dtest.property=true`.
 */
class TestPropertyCondition : Condition {

    override fun matches(context: ConditionContext, metadata: AnnotatedTypeMetadata): Boolean {
        return context.environment.getProperty("test.property") == "true"
    }
}

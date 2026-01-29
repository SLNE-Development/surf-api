package dev.slne.surf.surfapi.shared.api.component.condition

/**
 * Interface for defining custom conditions that control component instantiation.
 *
 * Conditions are evaluated at runtime before a component is instantiated.
 * If the condition returns `false`, the component will not be loaded.
 *
 * Conditions are specified using the [@ConditionalOn][dev.slne.surf.surfapi.shared.api.component.requirement.ConditionalOn]
 * annotation on component classes.
 *
 * Example implementation:
 * ```kotlin
 * class FeatureEnabledCondition : ComponentCondition {
 *     override suspend fun evaluate(context: ComponentConditionContext): Boolean {
 *         return MyConfig.isFeatureEnabled()
 *     }
 * }
 *
 * @ConditionalOn(FeatureEnabledCondition::class)
 * @ComponentMeta
 * class MyFeatureComponent : AbstractComponent() { ... }
 * ```
 *
 * @see ComponentConditionContext
 * @see dev.slne.surf.surfapi.shared.api.component.requirement.ConditionalOn
 */
interface ComponentCondition {
    /**
     * Evaluates whether the component should be instantiated.
     *
     * @param context The context providing information about the component and environment
     * @return `true` if the component should be instantiated, `false` to skip it
     */
    suspend fun evaluate(context: ComponentConditionContext): Boolean
}

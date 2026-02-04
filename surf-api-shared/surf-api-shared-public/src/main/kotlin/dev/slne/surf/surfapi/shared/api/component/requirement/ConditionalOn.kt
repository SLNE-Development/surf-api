package dev.slne.surf.surfapi.shared.api.component.requirement

import dev.slne.surf.surfapi.shared.api.component.condition.ComponentCondition
import kotlin.reflect.KClass

/**
 * Specifies a custom condition that must be satisfied for the component to be loaded.
 *
 * The condition class must implement [ComponentCondition] and will be instantiated
 * and evaluated at runtime. If the condition returns `false`, the component will not be loaded.
 *
 * This annotation can be used on component classes directly or on meta-annotations.
 * It is repeatable, allowing multiple conditions to be specified.
 *
 * Example:
 * ```kotlin
 * class FeatureEnabledCondition : ComponentCondition {
 *     override suspend fun evaluate(context: ComponentConditionContext): Boolean {
 *         return Config.isFeatureEnabled()
 *     }
 * }
 *
 * @ConditionalOn(FeatureEnabledCondition::class)
 * @ComponentMeta
 * class MyComponent : AbstractComponent() { ... }
 * ```
 *
 * @property condition The condition class that determines if the component should be loaded
 *
 * @see ComponentCondition
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Repeatable
annotation class ConditionalOn(val condition: KClass<out ComponentCondition>)
package dev.slne.surf.surfapi.shared.api.component.requirement

import dev.slne.surf.surfapi.shared.api.component.Component
import kotlin.reflect.KClass

/**
 * Specifies that the component should only be loaded if another component is NOT present.
 *
 * This is useful for providing fallback implementations or default behavior
 * that should only be active when a more specific component is not available.
 *
 * This annotation can be used on component classes directly or on meta-annotations.
 * It is repeatable, allowing multiple missing component constraints to be specified.
 *
 * Example:
 * ```kotlin
 * // This component is only loaded if CustomLogger is not present
 * @ConditionalOnMissingComponent(CustomLogger::class)
 * @ComponentMeta
 * class DefaultLogger : AbstractComponent() { ... }
 * ```
 *
 * @property component The component class that must NOT be present for this component to load
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Repeatable
annotation class ConditionalOnMissingComponent(val component: KClass<out Component>)
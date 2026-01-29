package dev.slne.surf.surfapi.shared.api.component.requirement

/**
 * Specifies that the component should only be loaded in certain environments.
 *
 * The component will only be instantiated if the current environment matches
 * one of the specified environment names. This is useful for components that
 * should only run in development, production, or test environments.
 *
 * This annotation can be used on component classes directly or on meta-annotations.
 * It is repeatable, allowing multiple environment constraints to be specified.
 *
 * Example:
 * ```kotlin
 * @ConditionalOnEnvironment(environments = ["development", "test"])
 * @ComponentMeta
 * class DebugComponent : AbstractComponent() { ... }
 * ```
 *
 * @property environments Array of environment names in which the component should be loaded
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Repeatable
annotation class ConditionalOnEnvironment(val environments: Array<String>)

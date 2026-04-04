package dev.slne.surf.api.shared.api.component.requirement


/**
 * Specifies that the component depends on a class being available at runtime, using the class name as a string.
 *
 * This is an alternative to [@DependsOnClass][DependsOnClass] that uses a string class name
 * instead of a class reference. This is useful when the dependency class is not available
 * at compile time.
 *
 * The component will only be loaded if the specified class can be found in the classpath.
 *
 * This annotation can be used on component classes directly or on meta-annotations.
 * It is repeatable, allowing multiple class dependencies to be specified.
 *
 * Example:
 * ```kotlin
 * // Only load if a specific class is available
 * @DependsOnClassName("com.example.optional.OptionalFeature")
 * @ComponentMeta
 * class OptionalIntegration : AbstractComponent() { ... }
 * ```
 *
 * @property className The fully qualified class name that must be available
 *
 * @see DependsOnClass
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Repeatable
annotation class DependsOnClassName(val className: String)
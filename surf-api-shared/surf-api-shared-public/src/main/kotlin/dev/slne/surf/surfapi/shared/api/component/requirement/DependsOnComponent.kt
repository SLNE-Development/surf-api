package dev.slne.surf.surfapi.shared.api.component.requirement

import dev.slne.surf.surfapi.shared.api.component.Component
import kotlin.reflect.KClass

/**
 * Specifies that the component depends on another component being loaded first.
 *
 * This creates a dependency relationship that affects the loading order.
 * The dependent component will not be instantiated until all its dependencies
 * have been successfully loaded.
 *
 * Circular dependencies are detected at runtime and will cause an error with
 * a detailed message showing the cycle.
 *
 * This annotation can be used on component classes directly or on meta-annotations.
 * It is repeatable, allowing multiple component dependencies to be specified.
 *
 * Example:
 * ```kotlin
 * @ComponentMeta
 * class DatabaseComponent : AbstractComponent() { ... }
 *
 * @DependsOnComponent(DatabaseComponent::class)
 * @ComponentMeta
 * class UserService : AbstractComponent() {
 *     // DatabaseComponent is guaranteed to be loaded before this
 * }
 * ```
 *
 * @property component The component class that must be loaded before this component
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Repeatable
annotation class DependsOnComponent(val component: KClass<out Component>)

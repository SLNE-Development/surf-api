package dev.slne.surf.surfapi.shared.api.component.requirement

import kotlin.reflect.KClass

/**
 * Specifies that the component depends on a class being available at runtime.
 *
 * The component will only be loaded if the specified class can be found in the classpath.
 * This is useful for optional integrations with other libraries or plugins.
 *
 * This annotation can be used on component classes directly or on meta-annotations.
 * It is repeatable, allowing multiple class dependencies to be specified.
 *
 * Example:
 * ```kotlin
 * // Only load if LuckPerms API is available
 * @DependsOnClass(net.luckperms.api.LuckPerms::class)
 * @ComponentMeta
 * class LuckPermsIntegration : AbstractComponent() { ... }
 * ```
 *
 * @property clazz The class that must be available for the component to load
 *
 * @see DependsOnClassName
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Repeatable
annotation class DependsOnClass(val clazz: KClass<*>)
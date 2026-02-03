package dev.slne.surf.surfapi.shared.api.component.types

import dev.slne.surf.surfapi.shared.api.component.ComponentMeta

/**
 * Convenience meta-annotation to mark a class as a service component.
 *
 * This annotation serves as a semantic alias for [ComponentMeta] and
 * does not introduce any additional behavior by itself.
 * It exists to improve readability and express intent when defining
 * service-layer components.
 *
 * Any class annotated with `@Service` is automatically treated as a
 * component and managed by the component system.
 *
 * This annotation can also be used as a meta-annotation to define
 * custom service stereotypes with predefined configuration such as
 * priority or conditions.
 *
 * Example:
 * ```kotlin
 * @Service
 * class UserService : AbstractComponent() { ... }
 * ```
 *
 * @see ComponentMeta
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.RUNTIME)
@ComponentMeta
annotation class Service

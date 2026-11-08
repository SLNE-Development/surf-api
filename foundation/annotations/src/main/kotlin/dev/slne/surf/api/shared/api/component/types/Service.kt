package dev.slne.surf.api.shared.api.component.types

import dev.slne.surf.api.shared.api.component.SurfComponentMeta

/**
 * Convenience meta-annotation to mark a class as a service component.
 *
 * This annotation serves as a semantic alias for [SurfComponentMeta] and
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
 * @see SurfComponentMeta
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.RUNTIME)
@SurfComponentMeta
annotation class Service

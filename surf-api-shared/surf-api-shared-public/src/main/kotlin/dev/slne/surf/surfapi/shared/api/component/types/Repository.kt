package dev.slne.surf.surfapi.shared.api.component.types

import dev.slne.surf.surfapi.shared.api.component.ComponentMeta

/**
 * Convenience meta-annotation to mark a class as a repository component.
 *
 * This annotation does not add any additional behavior on its own.
 * It exists purely for semantic clarity and naming consistency,
 * allowing repository classes to be distinguished from other components.
 *
 * Internally, this annotation is treated exactly like [ComponentMeta].
 * Any class annotated with `@Repository` will be discovered and managed
 * by the component system.
 *
 * This annotation can also be used as a meta-annotation to create
 * more specialized repository stereotypes.
 *
 * Example:
 * ```kotlin
 * @Repository
 * class UserRepository : AbstractComponent() { ... }
 * ```
 *
 * @see ComponentMeta
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.RUNTIME)
@ComponentMeta
annotation class Repository

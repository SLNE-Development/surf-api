package dev.slne.surf.api.di

/**
 * Marks a class as a bean, or marks an annotation as one that makes the classes it annotates beans.
 *
 * A bean is any class the container may create and inject. It needs no supertype: a class becomes a
 * bean by carrying this annotation, or an annotation that carries it. [Service] and [Repository] are
 * exactly that — meta-annotated aliases that read better at the call site — and a project can define
 * its own the same way:
 *
 * ```kotlin
 * @BeanMeta
 * @Priority(100)
 * @Target(AnnotationTarget.CLASS)
 * annotation class Projection
 *
 * @Projection
 * class BalanceProjection(private val cache: BalanceCache)
 * ```
 *
 * Lifecycle is separate and opt-in through [SurfComponent]: a bean that needs to load, start or shut
 * down implements it, and a plain value or service does not have to.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class BeanMeta

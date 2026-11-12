package dev.slne.surf.api.di

/**
 * Orders beans that the dependency graph leaves unordered, lower values first.
 *
 * Constructor parameters already order everything that depends on something else, so a priority
 * only decides between beans that have no path between them — two independent listeners whose
 * registration order happens to matter, for instance. It cannot override the graph: a bean is never
 * created before something it depends on, whatever its priority says.
 *
 * On a [BeanPostProcessor] it orders the chain instead: every processor sees each bean in priority
 * order.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.PROPERTY, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Priority(val value: Int = 0)

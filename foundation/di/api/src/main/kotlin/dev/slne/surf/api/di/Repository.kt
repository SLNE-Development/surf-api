package dev.slne.surf.api.di

/**
 * Marks a class as a bean whose job is storing and retrieving data, with an optional [scope].
 *
 * Behaves exactly like [Service]; the separate name only says what the bean is for.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.RUNTIME)
@BeanMeta
annotation class Repository(val scope: Scope = Scope.SINGLETON)

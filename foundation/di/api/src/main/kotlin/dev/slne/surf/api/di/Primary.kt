package dev.slne.surf.api.di

/**
 * Picks this bean when several provide the same type and the injection point names none.
 *
 * At most one bean per provided type may carry it; a second one is a configuration error the
 * container reports rather than resolving arbitrarily. An injection point with a [Qualifier] still
 * gets the bean it named.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Primary

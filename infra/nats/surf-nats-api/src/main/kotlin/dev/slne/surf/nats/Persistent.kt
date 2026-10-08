package dev.slne.surf.nats

/**
 * Enables or disables the use of jet streams for the annotated class or function.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Persistent(val enabled: Boolean = true)

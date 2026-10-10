package dev.slne.surf.nats.rpc

/**
 * Enables or disables the use of jet streams for the annotated class or function.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
public annotation class NatsPersistent(val enabled: Boolean = true)

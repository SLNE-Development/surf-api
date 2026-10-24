package dev.slne.surf.nats

/**
 * Marks internal implementation API without compatibility guarantees.
 */
@RequiresOptIn(
    level = RequiresOptIn.Level.ERROR,
    message = "Internal implementation API without compatibility guarantees."
)
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.TYPEALIAS,
)
public annotation class InternalNatsApi

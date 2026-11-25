package dev.slne.surf.nats

/**
 * Marks declarations that are public only for the surf-nats modules themselves.
 *
 * Such declarations have no compatibility guarantees, are excluded from ABI validation and can
 * change or disappear in any release. Application code must not opt in.
 */
@InternalApiMarker
@RequiresOptIn(
    level = RequiresOptIn.Level.ERROR,
    message = "Internal implementation API without compatibility guarantees.",
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

/**
 * Marks [InternalNatsApi] as an internal API marker for surf-api's `checkInternalApiUsage`.
 *
 * On the JVM it is surf-api's `InternalAPIMarker`; the other targets have no actual.
 */
@OptIn(ExperimentalMultiplatform::class)
@OptionalExpectation
@Target(AnnotationTarget.ANNOTATION_CLASS)
internal expect annotation class InternalApiMarker()

package dev.slne.surf.api.shared.api.annotation

/**
 * Marker annotation that designates an annotation class as an Internal API marker.
 *
 * Annotation classes annotated with [InternalAPIMarker] are recognized by the IntelliJ plugin
 * to identify internal API boundaries. Declarations annotated with such an annotation will be
 * treated as invisible to consumers in other projects — similar to Kotlin's
 * [DeprecationLevel.HIDDEN] behavior — rather than producing an opt-in warning or error.
 *
 * Example:
 * ```kotlin
 * @RequiresOptIn
 * @InternalAPIMarker
 * annotation class InternalSurfAPI
 *
 * @InternalSurfAPI
 * fun internalFunction() { ... }
 * ```
 */
@Target(AnnotationTarget.ANNOTATION_CLASS)
annotation class InternalAPIMarker
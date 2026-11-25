package dev.slne.surf.api.shared.api.annotation

/**
 * Stand-in for surf-api's meta-annotation, which the Gradle plugin cannot depend on; the check
 * matches it by name.
 */
@Target(AnnotationTarget.ANNOTATION_CLASS)
annotation class InternalAPIMarker

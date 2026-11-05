package dev.slne.surf.microservice.api

import dev.slne.surf.api.shared.api.annotation.InternalAPIMarker

/**
 * Marks declarations that only the surf-microservice runtime and its integrations may use.
 */
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.FUNCTION,
)
@Retention(AnnotationRetention.BINARY)
@RequiresOptIn
@InternalAPIMarker
annotation class InternalMicroserviceApi

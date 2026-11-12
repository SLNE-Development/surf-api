package dev.slne.surf.api.shared.api.util

import dev.slne.surf.api.shared.api.annotation.InternalAPIMarker

@RequiresOptIn(
    "This API is internal and should not be used from outside the library",
    RequiresOptIn.Level.ERROR
)
@InternalAPIMarker
annotation class InternalSurfApi

/**
 * Marks the hidden-class invoker infrastructure as internal and unstable.
 *
 * This API is intended **only** for surf-* plugins (surf-redis, surf-rabbitmq, etc.)
 * that need high-performance, annotation-driven listener dispatch via JVM hidden classes.
 *
 * **No API stability guarantees apply.** Breaking changes may happen at any time.
 * Consumer plugins must NOT depend on this API.
 */
@RequiresOptIn(
    "This is internal invoker infrastructure for surf-* plugins. " +
            "No API stability guarantees. Do not use from consumer plugins.",
    RequiresOptIn.Level.ERROR
)
@InternalAPIMarker
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.TYPEALIAS
)
annotation class InternalInvokerApi
package dev.slne.surf.api.shared.api.util

import dev.slne.surf.api.shared.api.annotation.InternalAPIMarker

/**
 * Marks a declaration that only surf-api's own modules use.
 *
 * It may change in any release: the ABI dumps leave it out, so `checkKotlinAbi` does not guard it.
 * A declaration that a public `inline` function references ends up in the bytecode of the plugins
 * that call that function; mark it [PublishedSurfApi] instead. `checkInlineInternalApi` fails the
 * build when a public `inline` function references a declaration that carries only this marker.
 */
@RequiresOptIn(
    "This API is internal and should not be used from outside the library",
    RequiresOptIn.Level.ERROR
)
@InternalAPIMarker
annotation class InternalSurfApi

/**
 * Marks a declaration that is internal to surf-api but referenced from a public `inline` function.
 *
 * Plugins must not call it directly, but the inlined code they compile does, so it is part of the
 * binary contract: the ABI dumps include it, and it changes only as public API does.
 */
@RequiresOptIn(
    "This API is internal to surf-api and should not be used from outside the library",
    RequiresOptIn.Level.ERROR
)
@InternalAPIMarker
annotation class PublishedSurfApi

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
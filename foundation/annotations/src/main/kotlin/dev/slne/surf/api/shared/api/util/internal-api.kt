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
    RequiresOptIn.Level.ERROR,
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
    RequiresOptIn.Level.ERROR,
)
@InternalAPIMarker
annotation class PublishedSurfApi

package dev.slne.surf.api.shared.api.util

import dev.slne.surf.api.shared.api.annotation.InternalAPIMarker

/**
 * Stand-in for surf-api's marker, which the Gradle plugin cannot depend on; the check matches it
 * by name.
 */
@InternalAPIMarker
annotation class InternalSurfApi

/**
 * Stand-in for surf-api's marker, which the Gradle plugin cannot depend on; the check matches it
 * by name.
 */
@InternalAPIMarker
annotation class PublishedSurfApi

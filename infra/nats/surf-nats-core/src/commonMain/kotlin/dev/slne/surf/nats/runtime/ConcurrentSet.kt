package dev.slne.surf.nats.runtime

import dev.slne.surf.nats.InternalNatsApi

/**
 * Creates an empty mutable set that is safe for concurrent use on the platform.
 */
@InternalNatsApi
internal expect fun <T> concurrentSetOf(): MutableSet<T>

package dev.slne.surf.nats.runtime

import dev.slne.surf.nats.InternalNatsApi

// Single-threaded
@InternalNatsApi
internal actual fun <T> concurrentSetOf(): MutableSet<T> = mutableSetOf()

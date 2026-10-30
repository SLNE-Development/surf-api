package dev.slne.surf.nats.runtime

import dev.slne.surf.nats.InternalNatsApi

@InternalNatsApi
internal expect fun <T> concurrentSetOf(): MutableSet<T>

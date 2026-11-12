package dev.slne.surf.nats.runtime

import dev.slne.surf.nats.InternalNatsApi
import java.util.concurrent.ConcurrentHashMap

@InternalNatsApi
internal actual fun <T> concurrentSetOf(): MutableSet<T> = ConcurrentHashMap.newKeySet()

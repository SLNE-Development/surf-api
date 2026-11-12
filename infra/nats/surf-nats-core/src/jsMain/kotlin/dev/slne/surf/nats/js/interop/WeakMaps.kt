package dev.slne.surf.nats.js.interop

import js.collections.WeakMap

internal inline fun <K : Any, V : Any> WeakMap<K, V>.getOrPut(key: K, create: () -> V): V {
    get(key)?.let { return it }
    return create().also { set(key, it) }
}

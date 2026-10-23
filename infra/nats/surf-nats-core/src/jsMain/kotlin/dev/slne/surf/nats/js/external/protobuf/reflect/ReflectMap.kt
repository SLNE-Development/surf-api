@file:JsModule("@bufbuild/protobuf/reflect")

package dev.slne.surf.nats.js.external.protobuf.reflect

import dev.slne.surf.nats.js.external.protobuf.DescFieldMap
import js.iterable.JsIterator

// Keys are converted to the closest ECMAScript type of the key scalar: string, number, boolean or bigint.
internal external interface ReflectMap<K : Any, V> {
    val size: Int
    fun field(): DescFieldMap
    fun keys(): JsIterator<K>
    fun has(key: K): Boolean
    fun get(key: K): V?
    fun set(key: K, value: V): ReflectMap<K, V>
}

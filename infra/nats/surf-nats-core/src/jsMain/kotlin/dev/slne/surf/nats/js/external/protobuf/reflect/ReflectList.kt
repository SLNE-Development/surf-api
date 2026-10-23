@file:JsModule("@bufbuild/protobuf/reflect")

package dev.slne.surf.nats.js.external.protobuf.reflect

import dev.slne.surf.nats.js.external.protobuf.DescFieldList

internal external interface ReflectList<V> {
    val size: Int
    fun field(): DescFieldList
    fun get(index: Int): V?
    fun set(index: Int, item: V)
}

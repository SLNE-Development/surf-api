@file:JsModule("@nats-io/obj")

package dev.slne.surf.nats.js.external.nats.obj

import dev.slne.surf.nats.js.external.nats.core.NatsConnection
import kotlin.js.Promise

internal external class Objm(nc: NatsConnection) {
    fun create(name: String, opts: ObjectStoreOptions = definedExternally): Promise<ObjectStore>
    fun open(name: String, check: Boolean = definedExternally): Promise<ObjectStore>
}

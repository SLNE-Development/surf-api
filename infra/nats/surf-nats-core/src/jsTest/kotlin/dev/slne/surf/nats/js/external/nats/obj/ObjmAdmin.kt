@file:JsModule("@nats-io/obj")

package dev.slne.surf.nats.js.external.nats.obj

import dev.slne.surf.nats.js.external.nats.core.NatsConnection
import kotlin.js.Promise

// Objm with the bucket status the e2e tests read.
@JsName("Objm")
internal external class ObjmAdmin(nc: NatsConnection) {
    fun open(name: String, check: Boolean = definedExternally): Promise<ObjectStoreAdmin>
}

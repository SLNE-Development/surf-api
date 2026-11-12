@file:JsModule("@nats-io/obj")

package dev.slne.surf.nats.js.external.nats.obj

import kotlin.js.Promise

internal external interface ObjectStoreAdmin {
    fun status(): Promise<ObjectStoreStatus>
}

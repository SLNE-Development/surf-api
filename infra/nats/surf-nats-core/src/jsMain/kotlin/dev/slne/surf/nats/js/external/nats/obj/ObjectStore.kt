@file:JsModule("@nats-io/obj")

package dev.slne.surf.nats.js.external.nats.obj

import js.typedarrays.Uint8Array
import kotlin.js.Promise

internal external interface ObjectStore {
    fun getBlob(name: String): Promise<Uint8Array<*>?>
    fun putBlob(meta: ObjectStoreMeta, data: Uint8Array<*>?): Promise<ObjectInfo>
    fun delete(name: String): Promise<PurgeResponse>
}

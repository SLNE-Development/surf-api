@file:JsModule("@bufbuild/protobuf")

package dev.slne.surf.nats.js.external.protobuf

import js.typedarrays.Uint8Array

internal external interface UnknownField {
    val no: Int

    // WireType
    val wireType: Int
    val data: Uint8Array<*>
}

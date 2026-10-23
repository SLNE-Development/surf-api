@file:JsModule("@bufbuild/protobuf")

package dev.slne.surf.nats.js.external.protobuf

internal external interface DescFieldMap : DescField {
    // scalar | enum | message
    val mapKind: String

    // ScalarType of the key
    val mapKey: Int
}

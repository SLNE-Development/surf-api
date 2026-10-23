@file:JsModule("@bufbuild/protobuf")

package dev.slne.surf.nats.js.external.protobuf

internal external interface DescFieldList : DescField {
    // scalar | enum | message
    val listKind: String
}

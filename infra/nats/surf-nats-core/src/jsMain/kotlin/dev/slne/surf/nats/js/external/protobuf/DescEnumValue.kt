@file:JsModule("@bufbuild/protobuf")

package dev.slne.surf.nats.js.external.protobuf

internal external interface DescEnumValue : AnyDesc {
    val name: String
    val localName: String
    val number: Int
}

@file:JsModule("@bufbuild/protobuf")

package dev.slne.surf.nats.js.external.protobuf

internal external interface DescEnum : AnyDesc {
    val typeName: String
    val name: String
    val open: Boolean
    val values: Array<DescEnumValue>
}

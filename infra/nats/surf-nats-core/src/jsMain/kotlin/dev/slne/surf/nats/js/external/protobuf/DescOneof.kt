@file:JsModule("@bufbuild/protobuf")

package dev.slne.surf.nats.js.external.protobuf

internal external interface DescOneof : AnyDesc {
    val name: String
    val localName: String
    val parent: DescMessage
    val fields: Array<DescField>
}

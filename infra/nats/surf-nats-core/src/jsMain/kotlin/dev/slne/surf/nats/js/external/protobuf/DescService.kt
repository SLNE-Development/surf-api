@file:JsModule("@bufbuild/protobuf")

package dev.slne.surf.nats.js.external.protobuf

internal external interface DescService : AnyDesc {
    val typeName: String
    val name: String
    val file: DescFile
    val methods: Array<DescMethod>
}

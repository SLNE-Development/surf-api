@file:JsModule("@bufbuild/protobuf")

package dev.slne.surf.nats.js.external.protobuf

internal external interface DescFile : AnyDesc {
    val name: String
    val dependencies: Array<DescFile>
    val extensions: Array<DescExtension>
    val proto: FileDescriptorProto
}

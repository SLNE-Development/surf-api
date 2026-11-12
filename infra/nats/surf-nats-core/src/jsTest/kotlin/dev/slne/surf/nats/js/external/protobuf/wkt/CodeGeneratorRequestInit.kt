package dev.slne.surf.nats.js.external.protobuf.wkt

import dev.slne.surf.nats.js.external.protobuf.FileDescriptorProto
import kotlinx.js.JsPlainObject

@JsPlainObject
internal external interface CodeGeneratorRequestInit {
    val fileToGenerate: Array<String>
    val parameter: String
    val protoFile: Array<FileDescriptorProto>
}

@file:JsModule("@bufbuild/protobuf/wkt")

package dev.slne.surf.nats.js.external.protobuf.wkt

import dev.slne.surf.nats.js.api.ProtoMessage
import dev.slne.surf.nats.js.external.protobuf.FileDescriptorProto

internal external interface FileDescriptorSet : ProtoMessage {
    val file: Array<FileDescriptorProto>
}

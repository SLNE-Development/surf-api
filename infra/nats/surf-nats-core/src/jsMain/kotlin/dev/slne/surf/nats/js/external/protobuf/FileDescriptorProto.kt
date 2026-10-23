@file:JsModule("@bufbuild/protobuf")

package dev.slne.surf.nats.js.external.protobuf

import dev.slne.surf.nats.js.ProtoMessage

internal external interface FileDescriptorProto : ProtoMessage {
    val name: String
    val `package`: String
}

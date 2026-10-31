@file:JsModule("@bufbuild/protobuf/wkt")

package dev.slne.surf.nats.js.external.protobuf.wkt

import dev.slne.surf.nats.js.api.ProtoMessage

internal external interface CodeGeneratorResponseFile : ProtoMessage {
    val name: String
    val content: String
}

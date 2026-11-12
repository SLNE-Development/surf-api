@file:JsModule("@bufbuild/protobuf/wkt")

package dev.slne.surf.nats.js.external.protobuf.wkt

import dev.slne.surf.nats.js.api.ProtoMessage

/**
 * Binds `CodeGeneratorResponse` of `google/protobuf/compiler/plugin.proto`, the response of a plugin.
 */
internal external interface CodeGeneratorResponse : ProtoMessage {
    val file: Array<CodeGeneratorResponseFile>
}

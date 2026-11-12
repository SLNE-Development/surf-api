@file:JsModule("@bufbuild/protobuf/wkt")

package dev.slne.surf.nats.js.external.protobuf.wkt

import dev.slne.surf.nats.js.api.ProtoMessage

/**
 * Binds `CodeGeneratorRequest` of `google/protobuf/compiler/plugin.proto`, the request that protoc sends to a plugin.
 */
internal external interface CodeGeneratorRequest : ProtoMessage

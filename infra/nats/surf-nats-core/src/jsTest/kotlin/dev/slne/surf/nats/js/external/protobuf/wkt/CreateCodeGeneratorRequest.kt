@file:JsModule("@bufbuild/protobuf")

package dev.slne.surf.nats.js.external.protobuf.wkt

import dev.slne.surf.nats.js.external.protobuf.DescMessage

// create() of @bufbuild/protobuf, typed for CodeGeneratorRequestSchema
@JsName("create")
internal external fun createCodeGeneratorRequest(schema: DescMessage, init: CodeGeneratorRequestInit): CodeGeneratorRequest

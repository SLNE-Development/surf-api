package dev.slne.surf.nats.js.external.protobuf.protoplugin

import dev.slne.surf.nats.js.external.protobuf.wkt.CodeGeneratorRequest
import dev.slne.surf.nats.js.external.protobuf.wkt.CodeGeneratorResponse

internal external interface Plugin {
    val name: String
    val version: String

    // Throws for invalid plugin options and for errors of generateTs
    fun run(request: CodeGeneratorRequest): CodeGeneratorResponse
}

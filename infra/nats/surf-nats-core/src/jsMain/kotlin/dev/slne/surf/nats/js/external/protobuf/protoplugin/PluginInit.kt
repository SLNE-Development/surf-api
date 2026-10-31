package dev.slne.surf.nats.js.external.protobuf.protoplugin

import kotlinx.js.JsPlainObject

// Without generateJs and generateDts, protoplugin transpiles the files of generateTs for the js and dts targets
@JsPlainObject
internal external interface PluginInit {
    val name: String
    val version: String
    val generateTs: (schema: Schema, target: String) -> Unit
}

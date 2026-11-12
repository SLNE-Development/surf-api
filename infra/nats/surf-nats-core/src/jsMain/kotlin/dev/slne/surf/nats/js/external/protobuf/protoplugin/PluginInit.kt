package dev.slne.surf.nats.js.external.protobuf.protoplugin

import kotlinx.js.JsPlainObject

/**
 * Binds the `init` argument of `createEcmaScriptPlugin`, limited to the members that surf-nats sets.
 *
 * Without `generateJs` and `generateDts`, protoplugin transpiles the files of `generateTs` for the `js` and `dts`
 * targets.
 */
@JsPlainObject
internal external interface PluginInit {
    val name: String
    val version: String
    val generateTs: (schema: Schema, target: String) -> Unit
}

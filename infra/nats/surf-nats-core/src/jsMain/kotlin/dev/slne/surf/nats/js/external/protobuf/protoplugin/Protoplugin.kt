package dev.slne.surf.nats.js.external.protobuf.protoplugin

import dev.slne.surf.nats.js.external.protobuf.DescComments
import dev.slne.surf.nats.js.external.protobuf.DescMethod
import dev.slne.surf.nats.js.external.protobuf.DescService

// Namespace of @bufbuild/protoplugin, loaded with import() so that the client runtime never loads the TypeScript compiler.
// JsAny is the bound of the import() wrapper.
@OptIn(ExperimentalWasmJsInterop::class)
internal external interface Protoplugin : JsAny {
    fun createEcmaScriptPlugin(init: PluginInit): Plugin
    fun runNodeJs(plugin: Plugin)
    fun getComments(desc: DescService): DescComments
    fun getComments(desc: DescMethod): DescComments
}

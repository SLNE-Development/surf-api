package dev.slne.surf.nats.js.protoc

import dev.slne.surf.nats.NatsConstants
import dev.slne.surf.nats.js.external.protobuf.protoplugin.Plugin
import dev.slne.surf.nats.js.external.protobuf.protoplugin.PluginInit
import dev.slne.surf.nats.js.external.protobuf.protoplugin.Protoplugin
import dev.slne.surf.nats.js.external.protobuf.protoplugin.Schema
import dev.slne.surf.nats.protocol.ProtocolOptions

/**
 * The protoc plugin `protoc-gen-surf-nats`: generates a typed client per surf-nats service on top of the
 * protoc-gen-es output.
 *
 * Plugin options, targets and the response come from `@bufbuild/protoplugin`. Services are validated with the rules
 * and messages of the Kotlin generator. Any violation fails the whole run with an error that lists every problem, one
 * per line, and no files.
 *
 * Only the files to generate are validated and emitted, not their imports. A file that declares services yields
 * `<file>_nats.ts`, transpiled to `.js` and `.d.ts` according to the `target` option. A file without services yields
 * nothing. [ClientEmitter] describes the content of a generated file.
 */
internal object ProtocPlugin {
    const val NAME = "protoc-gen-surf-nats"

    /**
     * npm package of protoplugin. It is loaded when the plugin runs, so that the client never loads it.
     */
    const val MODULE = "@bufbuild/protoplugin"

    /**
     * Plugin version in the generated preamble and in the output of `--version`: `v` plus the surf-nats version.
     */
    val version: String get() = "v${NatsConstants.VERSION}"

    /**
     * Creates the plugin with the functions of [protoplugin], the loaded [MODULE].
     */
    fun create(protoplugin: Protoplugin): Plugin = protoplugin.createEcmaScriptPlugin(
        PluginInit(name = NAME, version = version, generateTs = { schema, _ -> generate(protoplugin, schema) })
    )

    private fun generate(protoplugin: Protoplugin, schema: Schema) {
        val optionExtensions = schema.allFiles.firstOrNull { it.proto.name == ProtocolOptions.PROTO_FILE }
            ?.extensions?.toList().orEmpty()
        val problems = mutableListOf<String>()
        val resolver = ClientResolver(protoplugin, problems, optionExtensions)
        val clients = schema.files.mapNotNull(resolver::resolve)
        require(problems.isEmpty()) { problems.joinToString("\n") }
        for (client in clients) ClientEmitter(schema, client).emit()
    }
}

@file:OptIn(ExperimentalJsExport::class)

package dev.slne.surf.nats.js.protoc

import dev.slne.surf.nats.js.client.apiScope
import dev.slne.surf.nats.js.external.protobuf.protoplugin.Protoplugin
import js.import.import
import kotlinx.coroutines.promise
import kotlin.js.Promise

/**
 * Runs `protoc-gen-surf-nats` in the current Node.js process with `runNodeJs` of `@bufbuild/protoplugin`: reads a
 * `CodeGeneratorRequest` from stdin and writes the `CodeGeneratorResponse` to stdout.
 *
 * `--version` (or `-v`) prints the plugin name and version. Invalid contracts, invalid plugin options and other
 * arguments print to stderr and exit with code 1. The returned promise resolves once the plugin has started, and
 * `runNodeJs` exits the process.
 *
 * Called by `bin/protoc-gen-surf-nats.mjs`, the `protoc-gen-surf-nats` binary of the npm package. It is not part of
 * the `index.d.mts` facade.
 */
@JsExport
fun runProtocGenSurfNats(): Promise<Unit> = apiScope.promise {
    val protoplugin = import<Protoplugin>(ProtocPlugin.MODULE)
    protoplugin.runNodeJs(ProtocPlugin.create(protoplugin))
}

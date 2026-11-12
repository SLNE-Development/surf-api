@file:OptIn(ExperimentalJsExport::class)

package dev.slne.surf.nats.js.api

/**
 * A protobuf-es method descriptor, such as `PlayerService.method.findPlayer` from protoc-gen-es.
 *
 * Only [kind] `"rpc"` is accepted.
 */
@JsExport
external interface MethodSchema {
    val kind: String
    val name: String
}

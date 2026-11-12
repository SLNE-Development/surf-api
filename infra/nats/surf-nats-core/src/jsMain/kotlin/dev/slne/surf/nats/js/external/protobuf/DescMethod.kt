@file:JsModule("@bufbuild/protobuf")

package dev.slne.surf.nats.js.external.protobuf

import dev.slne.surf.nats.js.api.MethodSchema

/**
 * Binds `DescMethod`, which describes an rpc.
 *
 * The descriptor `<Service>.method.<name>` of the generated code is passed to the exported API as [MethodSchema].
 */
internal external interface DescMethod : AnyDesc, MethodSchema {
    override val name: String

    /**
     * Name of the rpc as a property name in ECMAScript, in lowerCamelCase.
     */
    val localName: String

    /**
     * One of `unary`, `server_streaming`, `client_streaming` and `bidi_streaming`.
     */
    val methodKind: String
    val parent: DescService
    val input: DescMessage
    val output: DescMessage
}

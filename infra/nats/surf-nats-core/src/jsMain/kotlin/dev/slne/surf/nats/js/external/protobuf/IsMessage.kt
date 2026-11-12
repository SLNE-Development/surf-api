@file:JsModule("@bufbuild/protobuf")

package dev.slne.surf.nats.js.external.protobuf

/**
 * Binds `isMessage`, which returns whether [arg] is a message, or a message of [schema] when it is given.
 *
 * A type guard in TypeScript (`arg is MessageShape<Desc>`). See `messageOf`.
 */
internal external fun isMessage(arg: Any?, schema: DescMessage = definedExternally): Boolean

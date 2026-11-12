@file:JsModule("@bufbuild/protobuf")

package dev.slne.surf.nats.js.external.protobuf

import dev.slne.surf.nats.js.api.ProtoMessage
import js.typedarrays.Uint8Array

/**
 * Binds `fromBinary`, which parses [bytes] in the protobuf binary format to a message of [schema].
 */
internal external fun fromBinary(
    schema: DescMessage,
    bytes: Uint8Array<*>,
    options: BinaryReadOptions = definedExternally,
): ProtoMessage

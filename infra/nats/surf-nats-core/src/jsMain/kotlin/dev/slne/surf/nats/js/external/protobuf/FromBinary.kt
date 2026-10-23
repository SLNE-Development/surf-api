@file:JsModule("@bufbuild/protobuf")

package dev.slne.surf.nats.js.external.protobuf

import dev.slne.surf.nats.js.ProtoMessage
import js.typedarrays.Uint8Array

internal external fun fromBinary(
    schema: DescMessage,
    bytes: Uint8Array<*>,
    options: BinaryReadOptions = definedExternally,
): ProtoMessage

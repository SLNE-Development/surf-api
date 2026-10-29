@file:OptIn(ExperimentalJsExport::class)

package dev.slne.surf.nats.js

import dev.slne.surf.nats.js.api.UuidMessage
import dev.slne.surf.nats.protocol.ProtocolFieldRules
import js.errors.RangeError
import js.typedarrays.Uint8Array
import js.typedarrays.toByteArray
import js.typedarrays.toUint8Array
import kotlin.uuid.Uuid

/**
 * Formats a `surf.types.v1.Uuid` message or its raw bytes as a lowercase `8-4-4-4-12` UUID string.
 *
 * @throws RangeError if the value does not hold exactly 16 bytes
 */
@JsExport
fun uuidToString(uuid: Any?): String {
    val raw: Any? = uuid as? Uint8Array<*> ?: uuid?.unsafeCast<UuidMessage>()?.value
    val bytes = raw as? Uint8Array<*>
    if (bytes == null || bytes.length != ProtocolFieldRules.UUID_BYTES) {
        throw RangeError(
            "${ProtocolFieldRules.UUID_TYPE} value must be exactly ${ProtocolFieldRules.UUID_BYTES} bytes, " +
                "was ${bytes?.length ?: raw}"
        )
    }

    return Uuid.fromByteArray(bytes.toByteArray()).toHexDashString()
}

/**
 * Parses an `8-4-4-4-12` UUID string (any case) into a `surf.types.v1.Uuid` message.
 *
 * @throws RangeError if [value] is not an `8-4-4-4-12` hex string
 */
@JsExport
fun uuidFromString(value: String?): UuidMessage {
    val uuid = value?.let(Uuid::parseHexDashOrNull)
        ?: throw RangeError("Invalid UUID '$value': expected 8-4-4-4-12 hexadecimal digits")

    return UuidMessage(
        `$typeName` = ProtocolFieldRules.UUID_TYPE,
        value = uuid.toByteArray().toUint8Array(),
    )
}

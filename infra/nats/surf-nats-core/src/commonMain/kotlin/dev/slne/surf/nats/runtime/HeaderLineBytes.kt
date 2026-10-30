package dev.slne.surf.nats.runtime

import dev.slne.surf.nats.InternalNatsApi
import dev.slne.surf.nats.protocol.ProtocolDefaults

/**
 * Returns the encoded size of a header line the platform client writes for [value]; `0` for a missing or empty value.
 */
@InternalNatsApi
internal fun headerLineBytes(name: String, value: String?): Int {
    return if (value.isNullOrEmpty()) 0 else ProtocolDefaults.headerLineBytes(name, value, HEADER_LINE_SEPARATOR_BYTES)
}

/**
 * Bytes the platform client writes per header line besides name and value.
 */
@InternalNatsApi
internal expect val HEADER_LINE_SEPARATOR_BYTES: Int

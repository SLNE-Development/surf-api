package dev.slne.surf.nats.js.codec

import dev.slne.surf.nats.js.external.nats.core.Msg
import dev.slne.surf.nats.js.external.nats.core.MsgHdrs
import dev.slne.surf.nats.protocol.ProtocolDefaults
import dev.slne.surf.nats.protocol.ProtocolHeaders
import dev.slne.surf.nats.runtime.writeBaseNatsHeaders
import js.buffer.ArrayBuffer
import js.typedarrays.Uint8Array
import dev.slne.surf.nats.js.external.nats.core.headers as createNatsHeaders

internal object NatsHeaders {

    // nats.js writes a header line as `\r\nname: value`
    private const val LINE_SEPARATOR_BYTES = 4

    val EMPTY_BODY: Uint8Array<*> = Uint8Array<ArrayBuffer>(0)

    fun forOrigin(origin: String): MsgHdrs {
        val headers = createNatsHeaders()
        writeBaseNatsHeaders(origin, headers::set)
        return headers
    }

    /**
     * Returns the size of the encoded header block, including the terminating `\r\n\r\n`.
     */
    fun serializedLength(headers: MsgHdrs): Int = headers.encode().length

    /**
     * Returns the encoded size of a header line nats.js writes for [value]; `0` for a missing or empty value.
     */
    fun lineBytes(name: String, value: String?): Int {
        return if (value.isNullOrEmpty()) 0 else ProtocolDefaults.headerLineBytes(name, value, LINE_SEPARATOR_BYTES)
    }
}

internal fun Msg.header(name: String): String? = ProtocolHeaders.valueOrNull(headers?.get(name))

// Status code of a status message (503 = no responders), 0 for regular messages
internal fun Msg.statusCode(): Int = headers?.code ?: 0

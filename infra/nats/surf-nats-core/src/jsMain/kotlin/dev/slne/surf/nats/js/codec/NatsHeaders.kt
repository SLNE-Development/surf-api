package dev.slne.surf.nats.js.codec

import dev.slne.surf.nats.js.external.nats.core.Msg
import dev.slne.surf.nats.js.external.nats.core.MsgHdrs
import dev.slne.surf.nats.protocol.ProtocolDefaults
import dev.slne.surf.nats.protocol.ProtocolHeaders
import dev.slne.surf.nats.runtime.writeBaseNatsHeaders
import dev.slne.surf.nats.js.external.nats.core.headers as createNatsHeaders

internal object NatsHeaders {
    /**
     * Bytes nats.js writes around each header line: `\r\n<name>: <value>`.
     */
    private const val LINE_OVERHEAD_BYTES = 4

    fun base(origin: String): MsgHdrs {
        val headers = createNatsHeaders()
        writeBaseNatsHeaders(origin, headers::set)
        return headers
    }

    fun get(message: Msg, name: String): String? {
        return ProtocolHeaders.valueOrNull(message.headers?.get(name))
    }

    fun statusCode(message: Msg): Int = message.headers?.code ?: 0

    /**
     * Size of the whole encoded header block, including the terminating `\r\n\r\n`.
     */
    fun encodedSize(headers: MsgHdrs): Int = headers.encode().length

    fun lineSize(name: String, value: String): Int {
        return ProtocolDefaults.headerLineBytes(name, value, LINE_OVERHEAD_BYTES)
    }
}

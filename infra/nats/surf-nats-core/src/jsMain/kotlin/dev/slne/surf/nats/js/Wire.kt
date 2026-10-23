package dev.slne.surf.nats.js

import dev.slne.surf.nats.js.external.nats.core.Msg
import dev.slne.surf.nats.js.external.nats.core.MsgHdrs
import dev.slne.surf.nats.js.external.nats.core.headers
import dev.slne.surf.nats.protocol.ProtocolDefaults
import dev.slne.surf.nats.protocol.ProtocolHeaders
import dev.slne.surf.nats.runtime.writeBaseHeaders
import js.buffer.ArrayBuffer
import js.typedarrays.Uint8Array
import web.encoding.TextDecoder

internal object Wire {
    // nats.js writes a header line as "\r\n<name>: <value>"
    private const val LINE_SEPARATOR_BYTES = 4

    fun emptyBody(): Uint8Array<*> = Uint8Array<ArrayBuffer>(0)

    fun baseHeaders(origin: String): MsgHdrs {
        val headers = headers()
        writeBaseHeaders(origin) { name, value -> headers.set(name, value) }
        return headers
    }

    fun header(message: Msg, name: String): String? = ProtocolHeaders.valueOrNull(message.headers?.get(name))

    // MsgHdrs.encode() is the whole header block, including the "\r\n\r\n" that ends it
    fun headerBytes(headers: MsgHdrs): Int = headers.encode().length

    // One header line as MsgHdrs.encode() writes it
    fun headerLineBytes(name: String, value: String): Int {
        return ProtocolDefaults.headerLineBytes(name, value, LINE_SEPARATOR_BYTES)
    }

    fun statusCode(message: Msg): Int = message.headers?.code ?: 0

    fun utf8(data: Uint8Array<*>): String = TextDecoder().decode(data)
}

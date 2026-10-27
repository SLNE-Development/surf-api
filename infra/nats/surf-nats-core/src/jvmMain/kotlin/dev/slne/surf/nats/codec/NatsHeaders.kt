package dev.slne.surf.nats.codec

import dev.slne.surf.nats.InternalNatsApi
import dev.slne.surf.nats.connection.NatsConnectionManager
import dev.slne.surf.nats.protocol.ProtocolDefaults
import dev.slne.surf.nats.protocol.ProtocolHeaders
import dev.slne.surf.nats.runtime.writeBaseNatsHeaders
import io.nats.client.Message
import io.nats.client.impl.Headers

@InternalNatsApi
internal object NatsHeaders {

    // jnats writes a header line as `name:value\r\n`
    private const val LINE_SEPARATOR_BYTES = 3

    val EMPTY_BODY = ByteArray(0)

    fun base(clientName: String): Headers {
        return forOrigin(origin(clientName))
    }

    fun forOrigin(origin: String): Headers {
        val headers = Headers()
        writeBaseNatsHeaders(origin, headers::add)
        return headers
    }

    fun origin(clientName: String) = ProtocolHeaders.origin(clientName, NatsConnectionManager.instanceId)

    /**
     * Returns the encoded size of a header line jnats writes for [value]; `0` for a missing or empty value.
     */
    fun lineBytes(name: String, value: String?): Int {
        return if (value.isNullOrEmpty()) 0 else ProtocolDefaults.headerLineBytes(name, value, LINE_SEPARATOR_BYTES)
    }
}

internal fun Message.header(name: String): String? = ProtocolHeaders.valueOrNull(headers?.getFirst(name))

internal fun Message.presenceKind(): String? = header(ProtocolHeaders.PRESENCE)

internal fun Message.deadlineMillis(): Long? = ProtocolHeaders.deadlineMillis(header(ProtocolHeaders.DEADLINE))

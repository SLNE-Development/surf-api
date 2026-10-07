package dev.slne.surf.nats.codec

import dev.slne.surf.nats.InternalNatsApi
import dev.slne.surf.nats.NatsConstants
import dev.slne.surf.nats.WireFormat
import dev.slne.surf.nats.connection.NatsConnectionManager
import io.nats.client.impl.Headers

@InternalNatsApi
object NatsHeaders {

    const val CONTENT_TYPE = "Content-Type"
    const val ORIGIN = "Surf-Origin"
    const val SENDER_VERSION = "Surf-Sender-Version"

    fun base(clientName: String, format: WireFormat): Headers {
        val headers = Headers()

        headers.add(CONTENT_TYPE, format.contentType)
        headers.add(SENDER_VERSION, NatsConstants.VERSION)
        headers.add(ORIGIN, "$clientName@${NatsConnectionManager.instanceId}")

        return headers
    }
}
package dev.slne.surf.nats.runtime

import dev.slne.surf.nats.InternalNatsApi
import dev.slne.surf.nats.NatsConstants
import dev.slne.surf.nats.protocol.ProtocolHeaders

/**
 * Writes the headers every surf-nats message carries.
 */
@InternalNatsApi
internal inline fun writeBaseHeaders(origin: String, writer: HeaderWriter) {
    writer(ProtocolHeaders.CONTENT_TYPE, ProtocolHeaders.PROTOBUF_CONTENT_TYPE)
    writer(ProtocolHeaders.SENDER_VERSION, NatsConstants.VERSION)
    writer(ProtocolHeaders.ORIGIN, origin)
}

@InternalNatsApi
internal typealias HeaderWriter = (name: String, value: String) -> Unit
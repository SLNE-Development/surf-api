package dev.slne.surf.nats.runtime

import dev.slne.surf.nats.InternalNatsApi
import dev.slne.surf.nats.NatsConstants
import dev.slne.surf.nats.protocol.ProtocolHeaders

/**
 * Writes the headers every surf-nats message carries.
 */
@InternalNatsApi
internal inline fun writeBaseHeaders(origin: String, set: (name: String, value: String) -> Unit) {
    set(ProtocolHeaders.CONTENT_TYPE, ProtocolHeaders.PROTOBUF_CONTENT_TYPE)
    set(ProtocolHeaders.SENDER_VERSION, NatsConstants.VERSION)
    set(ProtocolHeaders.ORIGIN, origin)
}

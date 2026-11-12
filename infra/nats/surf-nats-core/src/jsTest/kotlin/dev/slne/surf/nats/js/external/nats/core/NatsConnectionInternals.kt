package dev.slne.surf.nats.js.external.nats.core

/**
 * The nats.js connection implementation, which exposes its [protocol] handler.
 */
internal external interface NatsConnectionInternals {
    val protocol: NatsProtocol
}

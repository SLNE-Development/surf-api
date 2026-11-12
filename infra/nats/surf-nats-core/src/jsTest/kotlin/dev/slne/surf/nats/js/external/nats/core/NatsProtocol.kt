package dev.slne.surf.nats.js.external.nats.core

/**
 * The nats.js protocol handler behind a connection; its outbound buffer is only written to the socket while
 * [connected] is set.
 */
internal external interface NatsProtocol {
    var connected: Boolean
}

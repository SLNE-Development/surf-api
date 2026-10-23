@file:JsModule("@nats-io/transport-node")

package dev.slne.surf.nats.js.external.nats.core

// Status union of nats-core, discriminated by type; the variant members are undefined on the other variants.
internal external interface Status {
    // disconnect | reconnect | reconnecting | update | ldm | error | ping | staleConnection | slowConsumer |
    // forceReconnect | close
    val type: String

    // disconnect, reconnect, ldm
    val server: String?

    // error
    val error: Throwable?
}

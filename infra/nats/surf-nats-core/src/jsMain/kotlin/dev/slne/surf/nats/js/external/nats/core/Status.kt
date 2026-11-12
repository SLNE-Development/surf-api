@file:JsModule("@nats-io/transport-node")

package dev.slne.surf.nats.js.external.nats.core

/**
 * Binds the `Status` union of nats-core, discriminated by `type`. The members of a variant are `undefined` on the
 * other variants.
 */
internal external interface Status {
    /**
     * One of `disconnect`, `reconnect`, `reconnecting`, `update`, `ldm`, `error`, `ping`, `staleConnection`,
     * `slowConsumer`, `forceReconnect` and `close`.
     */
    val type: String

    /**
     * Server of the variants `disconnect`, `reconnect` and `ldm`.
     */
    val server: String?

    /**
     * Error of the variant `error`.
     */
    val error: Throwable?
}

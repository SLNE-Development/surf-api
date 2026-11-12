@file:OptIn(ExperimentalJsExport::class)

package dev.slne.surf.nats.js.error

import dev.slne.surf.nats.rpc.NatsRpcMessages

/**
 * Thrown when no instance serves a non-persistent `ANY_INSTANCE` method.
 *
 * @property subject the subject of the called method
 */
@JsExport
class NatsNoRespondersError internal constructor(
    val subject: String,
) : NatsRpcError(NatsRpcMessages.noResponders(subject))

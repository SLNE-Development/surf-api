@file:OptIn(ExperimentalJsExport::class)

package dev.slne.surf.nats.js.error

import dev.slne.surf.nats.rpc.NatsRpcMessages

@JsExport
class NatsNoRespondersError internal constructor(
    val subject: String,
) : NatsRpcError(NatsRpcMessages.noResponders(subject))

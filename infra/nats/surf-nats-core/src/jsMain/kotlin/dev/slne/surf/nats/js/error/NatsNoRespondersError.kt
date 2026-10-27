@file:OptIn(ExperimentalJsExport::class)

package dev.slne.surf.nats.js.error

@JsExport
class NatsNoRespondersError internal constructor(
    val subject: String,
) : NatsRpcError("No responders for $subject")

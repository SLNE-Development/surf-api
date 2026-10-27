@file:OptIn(ExperimentalJsExport::class)

package dev.slne.surf.nats.js.error

@JsExport
class NatsRemoteError internal constructor(
    val type: String,
    val remoteMessage: String,
) : NatsRpcError("$type: $remoteMessage")

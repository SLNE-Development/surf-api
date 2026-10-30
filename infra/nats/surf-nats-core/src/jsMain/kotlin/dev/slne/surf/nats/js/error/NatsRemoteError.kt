@file:OptIn(ExperimentalJsExport::class)

package dev.slne.surf.nats.js.error

import dev.slne.surf.nats.rpc.NatsRpcMessages

@JsExport
class NatsRemoteError internal constructor(
    val type: String,
    val remoteMessage: String,
) : NatsRpcError(NatsRpcMessages.remote(type, remoteMessage))

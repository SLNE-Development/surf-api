@file:JsModule("@nats-io/transport-node")

package dev.slne.surf.nats.js.external.nats.core

import kotlinx.js.JsPlainObject

// SubscriptionOptions (SubOpts<Msg>) of nats-core
@JsPlainObject
internal external interface SubscriptionOptions {
    val queue: String?
    val max: Int?
    val timeout: Int?
    val callback: ((error: Throwable?, message: Msg) -> Unit)?
}

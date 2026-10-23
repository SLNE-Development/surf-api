@file:JsModule("@nats-io/transport-node")

package dev.slne.surf.nats.js.external.nats.core

// SubscriptionOptions (SubOpts<Msg>) of nats-core
internal external interface SubscriptionOptions {
    var queue: String?
    var max: Int?
    var timeout: Int?
    var callback: ((error: Throwable?, message: Msg) -> Unit)?
}

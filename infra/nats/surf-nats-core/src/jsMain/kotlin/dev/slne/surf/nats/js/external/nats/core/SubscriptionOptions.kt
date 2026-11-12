@file:JsModule("@nats-io/transport-node")

package dev.slne.surf.nats.js.external.nats.core

import kotlinx.js.JsPlainObject

/**
 * Binds `SubscriptionOptions` of nats-core, an alias of `SubOpts<Msg>`. `timeout` is milliseconds.
 */
@JsPlainObject
internal external interface SubscriptionOptions {
    val queue: String?
    val max: Int?
    val timeout: Int?
    val callback: ((error: Throwable?, message: Msg) -> Unit)?
}

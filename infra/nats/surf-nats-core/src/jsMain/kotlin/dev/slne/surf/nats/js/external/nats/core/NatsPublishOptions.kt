@file:JsModule("@nats-io/transport-node")

package dev.slne.surf.nats.js.external.nats.core

import kotlinx.js.JsPlainObject

/**
 * Binds `PublishOptions` of nats-core. The Kotlin name differs so that it does not collide with the exported
 * `PublishOptions` of the surf-nats API.
 */
@JsPlainObject
internal external interface NatsPublishOptions {
    val reply: String?
    val headers: MsgHdrs?
}

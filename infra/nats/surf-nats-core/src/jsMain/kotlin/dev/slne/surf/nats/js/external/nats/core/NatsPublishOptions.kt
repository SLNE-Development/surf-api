@file:JsModule("@nats-io/transport-node")

package dev.slne.surf.nats.js.external.nats.core

import kotlinx.js.JsPlainObject

// PublishOptions of nats-core
@JsPlainObject
internal external interface NatsPublishOptions {
    val reply: String?
    val headers: MsgHdrs?
}

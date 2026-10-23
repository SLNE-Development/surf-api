@file:JsModule("@nats-io/transport-node")

package dev.slne.surf.nats.js.external.nats.core

// PublishOptions of nats-core
internal external interface NatsPublishOptions {
    var reply: String?
    var headers: MsgHdrs?
}

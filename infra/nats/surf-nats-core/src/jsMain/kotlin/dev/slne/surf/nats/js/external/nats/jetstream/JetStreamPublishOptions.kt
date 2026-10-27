@file:JsModule("@nats-io/jetstream")

package dev.slne.surf.nats.js.external.nats.jetstream

import dev.slne.surf.nats.js.external.nats.core.MsgHdrs
import kotlinx.js.JsPlainObject

// Partial<JetStreamPublishOptions>
@JsPlainObject
internal external interface JetStreamPublishOptions {
    val msgID: String?
    val timeout: Int?
    val headers: MsgHdrs?
    val expect: StreamExpectations?
    val ttl: String?
    val retries: Int?
}

@file:JsModule("@nats-io/jetstream")

package dev.slne.surf.nats.js.external.nats.jetstream

import dev.slne.surf.nats.js.external.nats.core.MsgHdrs

// Partial<JetStreamPublishOptions>
internal external interface JetStreamPublishOptions {
    var msgID: String?
    var timeout: Int?
    var headers: MsgHdrs?
    var expect: StreamExpectations?
    var ttl: String?
    var retries: Int?
}

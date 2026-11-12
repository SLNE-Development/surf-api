@file:JsModule("@nats-io/jetstream")

package dev.slne.surf.nats.js.external.nats.jetstream

import dev.slne.surf.nats.js.external.nats.core.MsgHdrs
import kotlinx.js.JsPlainObject

/**
 * Binds `Partial<JetStreamPublishOptions>`.
 *
 * `msgID` is sent as the `Nats-Msg-Id` header, which the stream uses to detect duplicates. `timeout` is the
 * milliseconds to wait for the `PubAck`. `ttl` is a per-message TTL such as `1h` and applies only to streams that allow
 * message TTLs.
 */
@JsPlainObject
internal external interface JetStreamPublishOptions {
    val msgID: String?
    val timeout: Int?
    val headers: MsgHdrs?
    val expect: StreamExpectations?
    val ttl: String?
    val retries: Int?
}
